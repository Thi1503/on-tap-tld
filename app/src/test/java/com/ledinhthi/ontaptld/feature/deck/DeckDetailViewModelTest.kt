package com.ledinhthi.ontaptld.feature.deck

import androidx.lifecycle.SavedStateHandle
import com.ledinhthi.ontaptld.core.MainDispatcherRule
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.exception.GlobalExceptionHandler
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.DeleteDeckUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.DeleteFlashcardUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDeckUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveFlashcardsUseCase
import com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail.CardFilter
import com.ledinhthi.ontaptld.feature.deck.presentation.deckdetail.DeckDetailViewModel
import com.ledinhthi.ontaptld.navigation.ManualCardRoute
import com.ledinhthi.ontaptld.navigation.ReviewRoute
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeckDetailViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val observeDeck = mockk<ObserveDeckUseCase>()
    private val observeCards = mockk<ObserveFlashcardsUseCase>()
    private val deleteFlashcard = mockk<DeleteFlashcardUseCase>()
    private val deleteDeck = mockk<DeleteDeckUseCase>()
    private val clock = mockk<Clock>()
    private val navigator = mockk<AppNavigator>(relaxed = true)
    private val exceptionHandler = mockk<GlobalExceptionHandler>(relaxed = true)
    private val strings = mockk<StringProvider>(relaxed = true)
    private val toolbox = ViewModelToolbox(navigator, exceptionHandler, strings)

    private val now = 1_800_000_000_000L
    private val deck = Deck(DECK_ID, "IELTS", "#4F46E5", createdAt = 1, updatedAt = 1)

    /** [dueInDays] = 0: đến hạn từ 1 giờ trước và chưa ôn lần nào (thẻ mới). */
    private fun card(id: String, source: FlashcardSource, dueInDays: Int) = Flashcard(
        id = id,
        deckId = DECK_ID,
        noteId = null,
        source = source,
        question = "Q$id",
        answer = "A$id",
        dueDate = if (dueInDays == 0) now - HOUR else now + dueInDays * DAY,
        lastReviewedAt = if (dueInDays == 0) null else now - DAY,
        createdAt = 1,
        updatedAt = 1,
    )

    private val cards = listOf(
        card("a", FlashcardSource.AI, dueInDays = 0),
        card("b", FlashcardSource.MANUAL, dueInDays = 6),
        card("c", FlashcardSource.AI, dueInDays = 2),
        card("d", FlashcardSource.MANUAL, dueInDays = 0),
        card("e", FlashcardSource.AI, dueInDays = 15),
    )

    private fun viewModel(): DeckDetailViewModel {
        every { clock.nowMillis() } returns now
        return DeckDetailViewModel(
            toolbox = toolbox,
            savedState = SavedStateHandle(mapOf("deckId" to DECK_ID)),
            observeDeck = observeDeck,
            observeCards = observeCards,
            deleteFlashcard = deleteFlashcard,
            deleteDeck = deleteDeck,
            clock = clock,
        ).apply { isTestMode = true }
    }

    private fun givenLoaded() {
        every { observeDeck.invoke(DECK_ID) } returns flowOf(deck)
        every { observeCards.invoke(DECK_ID) } returns flowOf(cards)
    }

    @Test
    fun `nhan du lieu - dem dung tong, can on hom nay, the moi va theo nguon`() = runTest {
        givenLoaded()

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isLoaded)
        assertEquals(deck, state.deck)
        assertEquals(5, state.cards.size)
        assertEquals(2, state.dueCount)
        assertEquals(2, state.newCount)
        assertEquals(2, state.manualCount)
        assertEquals(3, state.aiCount)
    }

    @Test
    fun `chon chip loc - chi con the dung nguon, so lieu tong khong doi`() = runTest {
        givenLoaded()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onFilterChange(CardFilter.MANUAL)
        assertEquals(listOf("b", "d"), vm.uiState.value.visibleCards.map { it.id })

        vm.onFilterChange(CardFilter.AI)
        assertEquals(listOf("a", "c", "e"), vm.uiState.value.visibleCards.map { it.id })
        assertEquals(5, vm.uiState.value.cards.size)

        vm.onFilterChange(CardFilter.ALL)
        assertEquals(5, vm.uiState.value.visibleCards.size)
    }

    @Test
    fun `bo the khong con ton tai - tai xong nhung deck la null`() = runTest {
        every { observeDeck.invoke(DECK_ID) } returns flowOf(null)
        every { observeCards.invoke(DECK_ID) } returns flowOf(emptyList())

        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isLoaded)
        assertNull(vm.uiState.value.deck)
    }

    @Test
    fun `doc du lieu loi - bao loadFailed, thu lai thanh cong thi het loi`() = runTest {
        every { observeDeck.invoke(DECK_ID) } returns flowOf(deck)
        every { observeCards.invoke(DECK_ID) } returns flow { throw IllegalStateException("db") }

        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.loadFailed)

        every { observeCards.invoke(DECK_ID) } returns flowOf(cards)
        vm.onRetry()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.loadFailed)
        assertTrue(vm.uiState.value.isLoaded)
    }

    @Test
    fun `them the va sua the - mo man the thu cong voi dung tham so`() = runTest {
        givenLoaded()
        val vm = viewModel()

        vm.onAddCardClick()
        vm.onEditCard("c")

        verify { navigator.to(ManualCardRoute(DECK_ID)) }
        verify { navigator.to(ManualCardRoute(DECK_ID, cardId = "c")) }
    }

    @Test
    fun `bam On N the - mo phien on chi cua bo nay`() = runTest {
        givenLoaded()
        val vm = viewModel()

        vm.onReviewClick()

        verify { navigator.to(ReviewRoute(DECK_ID)) }
    }

    @Test
    fun `xoa the - goi use case va bao da xoa, van o lai man`() = runTest {
        givenLoaded()
        coEvery { deleteFlashcard.invoke("b") } returns Unit

        val vm = viewModel()
        vm.onDeleteCard("b")
        advanceUntilIdle()

        coVerify { deleteFlashcard.invoke("b") }
        verify { navigator.showSnackBar(any(), any()) }
        verify(exactly = 0) { navigator.back() }
    }

    @Test
    fun `xoa bo the - goi use case roi quay ve man truoc`() = runTest {
        givenLoaded()
        coEvery { deleteDeck.invoke(DECK_ID) } returns Unit

        val vm = viewModel()
        vm.onDeleteDeck()
        advanceUntilIdle()

        coVerify { deleteDeck.invoke(DECK_ID) }
        verify { navigator.back() }
    }

    @Test
    fun `xoa bo the loi - khong quay ve, de ExceptionHandler bao loi`() = runTest {
        givenLoaded()
        coEvery { deleteDeck.invoke(DECK_ID) } throws IllegalStateException("db")

        val vm = viewModel()
        vm.onDeleteDeck()
        advanceUntilIdle()

        verify(exactly = 0) { navigator.back() }
        verify { exceptionHandler.handle(any()) }
    }

    private companion object {
        const val DECK_ID = "d1"
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
    }
}
