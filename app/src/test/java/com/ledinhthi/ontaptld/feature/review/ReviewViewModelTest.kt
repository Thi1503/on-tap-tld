package com.ledinhthi.ontaptld.feature.review

import androidx.lifecycle.SavedStateHandle
import com.ledinhthi.ontaptld.core.MainDispatcherRule
import com.ledinhthi.ontaptld.core.exception.GlobalExceptionHandler
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.GetFlashcardUseCase
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade
import com.ledinhthi.ontaptld.feature.review.domain.model.CardSource
import com.ledinhthi.ontaptld.feature.review.domain.model.ExcerptLine
import com.ledinhthi.ontaptld.feature.review.domain.model.ReviewCard
import com.ledinhthi.ontaptld.feature.review.domain.usecase.GetDueReviewCardsUseCase
import com.ledinhthi.ontaptld.feature.review.domain.usecase.ReviewFlashcardUseCase
import com.ledinhthi.ontaptld.feature.review.presentation.ReviewPhase
import com.ledinhthi.ontaptld.feature.review.presentation.ReviewViewModel
import com.ledinhthi.ontaptld.navigation.HomeRoute
import com.ledinhthi.ontaptld.navigation.ManualCardRoute
import com.ledinhthi.ontaptld.navigation.SourceImageRoute
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val getDueCards = mockk<GetDueReviewCardsUseCase>()
    private val reviewFlashcard = mockk<ReviewFlashcardUseCase>()
    private val getFlashcard = mockk<GetFlashcardUseCase>()
    private val navigator = mockk<AppNavigator>(relaxed = true)
    private val exceptionHandler = mockk<GlobalExceptionHandler>(relaxed = true)
    private val strings = mockk<StringProvider>(relaxed = true)
    private val toolbox = ViewModelToolbox(navigator, exceptionHandler, strings)

    private val deck1 = Deck("d1", "IELTS", "#4F46E5", createdAt = 1, updatedAt = 1)
    private val deck2 = Deck("d2", "Sinh học", "#0D9488", createdAt = 1, updatedAt = 1)

    private fun flashcard(id: String, deckId: String, interval: Int = 0) = Flashcard(
        id = id,
        deckId = deckId,
        noteId = null,
        source = FlashcardSource.MANUAL,
        question = "Q$id",
        answer = "A$id",
        interval = interval,
        dueDate = 1,
        createdAt = 1,
        updatedAt = 1,
    )

    private val cards = listOf(
        ReviewCard(flashcard("a", "d1"), deck1),
        ReviewCard(flashcard("b", "d2"), deck2),
        ReviewCard(flashcard("c", "d1"), deck1),
    )

    /** Mỗi mức chấm cho ra một khoảng hẹn khác nhau, đủ để kiểm tra các nhóm "lịch ôn tiếp theo". */
    private fun givenGrading() {
        coEvery { reviewFlashcard.invoke(any()) } answers {
            val params = firstArg<ReviewFlashcardUseCase.Params>()
            val interval = when (params.grade) {
                ReviewGrade.FORGOT, ReviewGrade.HARD -> 1
                ReviewGrade.EASY -> 6
                ReviewGrade.VERY_EASY -> 15
            }
            flashcard(params.cardId, "d1", interval)
        }
    }

    private fun viewModel(deckId: String? = null): ReviewViewModel {
        return ReviewViewModel(
            toolbox = toolbox,
            savedState = SavedStateHandle(mapOf("deckId" to deckId)),
            getDueCards = getDueCards,
            reviewFlashcard = reviewFlashcard,
            getFlashcard = getFlashcard,
        ).apply { isTestMode = true }
    }

    private fun ReviewViewModel.answer(grade: ReviewGrade) {
        onShowAnswer()
        onGrade(grade)
    }

    @Test
    fun `mo phien - nap the den han va hien cau hoi dau tien`() = runTest {
        coEvery { getDueCards.invoke(null) } returns cards

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ReviewPhase.QUESTION, state.phase)
        assertEquals("a", state.current?.card?.id)
        assertEquals(1, state.position)
        assertEquals(3, state.total)
    }

    @Test
    fun `mo phien theo bo the - chi hoi the den han cua bo do`() = runTest {
        coEvery { getDueCards.invoke("d2") } returns cards.filter { it.deck.id == "d2" }

        val vm = viewModel(deckId = "d2")
        advanceUntilIdle()

        assertEquals(listOf("b"), vm.uiState.value.cards.map { it.card.id })
    }

    @Test
    fun `khong co the den han - chang EMPTY`() = runTest {
        coEvery { getDueCards.invoke(null) } returns emptyList()

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(ReviewPhase.EMPTY, vm.uiState.value.phase)
    }

    @Test
    fun `nap the loi - chang ERROR, thu lai thi vao phien`() = runTest {
        coEvery { getDueCards.invoke(null) } throws IllegalStateException("db")
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(ReviewPhase.ERROR, vm.uiState.value.phase)

        coEvery { getDueCards.invoke(null) } returns cards
        vm.onRetry()
        advanceUntilIdle()

        assertEquals(ReviewPhase.QUESTION, vm.uiState.value.phase)
    }

    @Test
    fun `chua lat the thi khong cham duoc`() = runTest {
        coEvery { getDueCards.invoke(null) } returns cards
        givenGrading()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onGrade(ReviewGrade.EASY)
        advanceUntilIdle()

        coVerify(exactly = 0) { reviewFlashcard.invoke(any()) }
        assertEquals("a", vm.uiState.value.current?.card?.id)
    }

    @Test
    fun `cham mot the - luu ket qua roi sang cau hoi cua the ke tiep`() = runTest {
        coEvery { getDueCards.invoke(null) } returns cards
        givenGrading()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onShowAnswer()
        assertEquals(ReviewPhase.ANSWER, vm.uiState.value.phase)
        vm.onGrade(ReviewGrade.EASY)
        advanceUntilIdle()

        coVerify { reviewFlashcard.invoke(ReviewFlashcardUseCase.Params("a", ReviewGrade.EASY)) }
        val state = vm.uiState.value
        assertEquals(ReviewPhase.QUESTION, state.phase)
        assertEquals("b", state.current?.card?.id)
        assertEquals(2, state.position)
    }

    @Test
    fun `cham het the - sang tong ket voi so lieu tung muc va lich on tiep theo`() = runTest {
        coEvery { getDueCards.invoke(null) } returns cards
        givenGrading()
        val vm = viewModel()
        advanceUntilIdle()

        vm.answer(ReviewGrade.FORGOT)
        advanceUntilIdle()
        vm.answer(ReviewGrade.EASY)
        advanceUntilIdle()
        vm.answer(ReviewGrade.VERY_EASY)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ReviewPhase.DONE, state.phase)
        assertEquals(3, state.results.size)
        assertEquals(1, state.forgotCount)
        assertEquals(1, state.gradeCount(ReviewGrade.EASY))
        assertEquals(2, state.reviewedDeckCount)
        assertEquals(1, state.schedule.tomorrow)
        assertEquals(1, state.schedule.soon)
        assertEquals(6, state.schedule.soonDays)
        assertEquals(1, state.schedule.later)
    }

    @Test
    fun `on lai the da quen - mo luot moi chi gom cac the vua cham Quen`() = runTest {
        coEvery { getDueCards.invoke(null) } returns cards
        givenGrading()
        val vm = viewModel()
        advanceUntilIdle()
        vm.answer(ReviewGrade.FORGOT)
        advanceUntilIdle()
        vm.answer(ReviewGrade.EASY)
        advanceUntilIdle()
        vm.answer(ReviewGrade.FORGOT)
        advanceUntilIdle()

        vm.onReviewForgotten()

        val state = vm.uiState.value
        assertEquals(ReviewPhase.QUESTION, state.phase)
        assertEquals(listOf("a", "c"), state.cards.map { it.card.id })
        assertEquals(1, state.position)
        assertEquals(0, state.results.size)
    }

    @Test
    fun `luu ket qua cham loi - van o the do, khong sang the sau`() = runTest {
        coEvery { getDueCards.invoke(null) } returns cards
        coEvery { reviewFlashcard.invoke(any()) } throws IllegalStateException("db")
        val vm = viewModel()
        advanceUntilIdle()

        vm.answer(ReviewGrade.EASY)
        advanceUntilIdle()

        assertEquals(ReviewPhase.ANSWER, vm.uiState.value.phase)
        assertEquals("a", vm.uiState.value.current?.card?.id)
        verify { exceptionHandler.handle(any()) }
    }

    @Test
    fun `quay lai tu man sua the - nap lai noi dung the dang on`() = runTest {
        coEvery { getDueCards.invoke(null) } returns cards
        coEvery { getFlashcard.invoke("a") } returns flashcard("a", "d1").copy(question = "Q đã sửa")
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEditCard()
        verify { navigator.to(ManualCardRoute("d1", "a")) }
        vm.onResume()
        advanceUntilIdle()

        assertEquals("Q đã sửa", vm.uiState.value.current?.card?.question)
        assertEquals(ReviewPhase.QUESTION, vm.uiState.value.phase)
    }

    @Test
    fun `xem ca anh - mo anh nguon cua the dang on, quay lai van dung the va nguon cu`() = runTest {
        val source = CardSource(capturedAt = 5_000, excerpt = listOf(ExcerptLine("– Pha S.", isSource = true)))
        coEvery { getDueCards.invoke(null) } returns listOf(cards[0].copy(source = source), cards[1])
        coEvery { getFlashcard.invoke("a") } returns flashcard("a", "d1")
        val vm = viewModel()
        advanceUntilIdle()
        vm.onShowAnswer()

        vm.onViewSourceImage()
        verify { navigator.to(SourceImageRoute("a")) }
        vm.onResume() // đóng màn xem ảnh, màn ôn hiện lại
        advanceUntilIdle()

        assertEquals(ReviewPhase.ANSWER, vm.uiState.value.phase)
        assertEquals("a", vm.uiState.value.current?.card?.id)
        assertEquals(source, vm.uiState.value.current?.source)
    }

    @Test
    fun `ve trang chu va dong man`() = runTest {
        coEvery { getDueCards.invoke(null) } returns emptyList()
        val vm = viewModel()
        advanceUntilIdle()
        assertNull(vm.uiState.value.current)

        vm.onGoHome()
        verify { navigator.replaceAll(HomeRoute) }
        vm.onClose()
        verify { navigator.back() }
    }
}
