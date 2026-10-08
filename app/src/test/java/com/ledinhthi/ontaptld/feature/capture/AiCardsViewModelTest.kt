package com.ledinhthi.ontaptld.feature.capture

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ledinhthi.ontaptld.core.MainDispatcherRule
import com.ledinhthi.ontaptld.core.exception.AiErrorKind
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.GlobalExceptionHandler
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import com.ledinhthi.ontaptld.feature.capture.domain.exception.CaptureException
import com.ledinhthi.ontaptld.feature.capture.domain.model.SuggestedCard
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.GenerateFlashcardsWithAiUseCase
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.SaveSuggestedCardsUseCase
import com.ledinhthi.ontaptld.feature.capture.presentation.aicards.AiCardsEffect
import com.ledinhthi.ontaptld.feature.capture.presentation.aicards.AiCardsPhase
import com.ledinhthi.ontaptld.feature.capture.presentation.aicards.AiCardsViewModel
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDeckUseCase
import com.ledinhthi.ontaptld.navigation.DeckDetailRoute
import com.ledinhthi.ontaptld.navigation.HomeRoute
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiCardsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val observeDeck = mockk<ObserveDeckUseCase>()
    private val generateFlashcards = mockk<GenerateFlashcardsWithAiUseCase>()
    private val saveSuggestedCards = mockk<SaveSuggestedCardsUseCase>()
    private val navigator = mockk<AppNavigator>(relaxed = true)
    private val exceptionHandler = mockk<GlobalExceptionHandler>(relaxed = true)
    private val strings = mockk<StringProvider>(relaxed = true)
    private val toolbox = ViewModelToolbox(navigator, exceptionHandler, strings)

    private val cards = listOf(
        SuggestedCard("Nhân đôi ADN diễn ra ở pha nào?", "Pha S.", sourceLine = 1),
        SuggestedCard("Đoạn Okazaki là gì?", "Đoạn ADN ngắn trên mạch chậm.", sourceLine = 4),
    )

    /** "Ngăn nhớ" mà hệ thống giữ hộ qua lần app bị tắt — đưa lại cho ViewModel mới để giả lập việc đó. */
    private fun newSavedState() = SavedStateHandle(
        mapOf("imagePath" to "/cache/crop.jpg", "noteText" to "Bài 1 · Nhân đôi ADN", "deckId" to "d1"),
    )

    private fun viewModel(savedState: SavedStateHandle = newSavedState()): AiCardsViewModel {
        every { observeDeck.invoke("d1") } returns
            flowOf(Deck("d1", "Sinh học 12", "#0D9488", createdAt = 1, updatedAt = 1))
        return AiCardsViewModel(
            toolbox = toolbox,
            savedState = savedState,
            observeDeck = observeDeck,
            generateFlashcards = generateFlashcards,
            saveSuggestedCards = saveSuggestedCards,
            json = Json,
        ).apply { isTestMode = true }
    }

    /** ViewModel đã có sẵn hai thẻ AI ở chặng duyệt — điểm xuất phát của các ca duyệt / lưu. */
    private fun reviewingViewModel(): AiCardsViewModel {
        coEvery { generateFlashcards.invoke(any()) } returns cards
        return viewModel()
    }

    // ---- Chờ AI ----

    @Test
    fun `mo man - gui van ban cho AI, co the thi sang chang duyet voi moi the duoc chon san`() = runTest {
        coEvery { generateFlashcards.invoke("Bài 1 · Nhân đôi ADN") } returns cards

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(AiCardsPhase.Suggestions, state.phase)
        assertEquals(listOf("Nhân đôi ADN diễn ra ở pha nào?", "Đoạn Okazaki là gì?"), state.items.map { it.question })
        assertEquals(listOf(1, 4), state.items.map { it.sourceLine })
        assertTrue(state.items.all { it.selected && it.fromAi })
        assertEquals(2, state.selectedCount)
        assertEquals("Sinh học 12", state.deckName)
    }

    @Test
    fun `huy luc dang cho - lui ve buoc 2, ket qua ve sau khong duoc dung`() = runTest {
        val pending = CompletableDeferred<List<SuggestedCard>>()
        coEvery { generateFlashcards.invoke(any()) } coAnswers { pending.await() }
        val vm = viewModel()
        assertEquals(AiCardsPhase.Generating, vm.uiState.value.phase)

        vm.onCancel()
        pending.complete(cards) // AI trả lời sau khi người dùng đã huỷ
        advanceUntilIdle()

        verify(exactly = 1) { navigator.back() }
        assertEquals(AiCardsPhase.Generating, vm.uiState.value.phase)
        assertTrue(vm.uiState.value.items.isEmpty())
        verify(exactly = 0) { exceptionHandler.handle(any()) }
    }

    @Test
    fun `goi AI loi - lui ve buoc 2 va de bo xu ly loi chung bao`() = runTest {
        coEvery { generateFlashcards.invoke(any()) } throws AppException.AiException(AiErrorKind.NETWORK)

        val vm = viewModel()
        advanceUntilIdle()

        verify { navigator.back() }
        verify { exceptionHandler.handle(match { (it.exception as? AppException.AiException)?.kind == AiErrorKind.NETWORK }) }
        assertEquals(AiCardsPhase.Generating, vm.uiState.value.phase)
    }

    // ---- Duyệt thẻ ----

    @Test
    fun `bo tich mot the - so the duoc chon giam, bo tich het thi khong luu duoc`() = runTest {
        val vm = reviewingViewModel()
        val (first, second) = vm.uiState.value.items

        vm.onToggleSelected(first.id)
        assertEquals(1, vm.uiState.value.selectedCount)
        assertTrue(vm.uiState.value.canSave)

        vm.onToggleSelected(second.id)
        assertEquals(0, vm.uiState.value.selectedCount)
        assertFalse(vm.uiState.value.canSave)

        vm.onToggleSelected(first.id) // tích lại
        assertTrue(vm.uiState.value.items.first().selected)
    }

    @Test
    fun `sua the - doi noi dung dung the do, van la the AI va giu dong nguon`() = runTest {
        val vm = reviewingViewModel()
        val second = vm.uiState.value.items[1]

        vm.onCardEdited(second.id, "  Okazaki là gì?  ", " Đoạn ngắn. ")

        val items = vm.uiState.value.items
        assertEquals("Okazaki là gì?", items[1].question)
        assertEquals("Đoạn ngắn.", items[1].answer)
        assertTrue(items[1].fromAi)
        assertEquals(4, items[1].sourceLine)
        assertEquals("Nhân đôi ADN diễn ra ở pha nào?", items[0].question) // thẻ kia không đổi
    }

    @Test
    fun `them the - nam cuoi danh sach, duoc chon san, danh dau la the tu go`() = runTest {
        val vm = reviewingViewModel()

        vm.onCardAdded(" Câu tự thêm? ", " Đáp tự thêm ")

        val items = vm.uiState.value.items
        assertEquals(3, items.size)
        val added = items.last()
        assertEquals("Câu tự thêm?", added.question)
        assertTrue(added.selected)
        assertFalse(added.fromAi)
        // Mỗi dòng một số riêng, kể cả dòng thêm sau.
        assertEquals(3, items.map { it.id }.distinct().size)
    }

    @Test
    fun `xoa the - bo khoi danh sach, xoa het van o lai man`() = runTest {
        val vm = reviewingViewModel()
        val (first, second) = vm.uiState.value.items

        vm.onDeleteCard(first.id)
        assertEquals(listOf(second.id), vm.uiState.value.items.map { it.id })

        vm.onDeleteCard(second.id)
        assertTrue(vm.uiState.value.items.isEmpty())
        assertEquals(AiCardsPhase.Suggestions, vm.uiState.value.phase)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `app bi he thong tat luc dang duyet - mo lai thay dung danh sach cu, khong goi AI lan nua`() = runTest {
        val savedState = newSavedState()
        coEvery { generateFlashcards.invoke(any()) } returns cards
        val first = viewModel(savedState)
        advanceUntilIdle()
        first.onToggleSelected(first.uiState.value.items[0].id)
        first.onCardAdded("Câu tự thêm?", "Đáp tự thêm")
        advanceUntilIdle()
        val before = first.uiState.value.items

        // ViewModel mới + cùng "ngăn nhớ" = đúng những gì xảy ra khi Android dựng lại app.
        val recreated = viewModel(savedState)
        advanceUntilIdle()

        assertEquals(AiCardsPhase.Suggestions, recreated.uiState.value.phase)
        assertEquals(before, recreated.uiState.value.items)
        coVerify(exactly = 1) { generateFlashcards.invoke(any()) } // chỉ lần của ViewModel đầu

        // Dòng thêm sau khi mở lại vẫn nhận số riêng, không đụng số của dòng cũ.
        recreated.onCardAdded("Câu nữa?", "Đáp nữa")
        assertEquals(4, recreated.uiState.value.items.map { it.id }.distinct().size)
    }

    // ---- Lưu ----

    @Test
    fun `luu - chi luu the dang tich, bao da luu roi mo bo the vua nhan the`() = runTest {
        coEvery { saveSuggestedCards.invoke(any()) } returns emptyList()
        val vm = reviewingViewModel()
        vm.onToggleSelected(vm.uiState.value.items[0].id) // bỏ thẻ đầu
        vm.onCardAdded("Câu tự thêm?", "Đáp tự thêm")

        vm.onSave()
        advanceUntilIdle()

        coVerify {
            saveSuggestedCards.invoke(
                SaveSuggestedCardsUseCase.Params(
                    deckId = "d1",
                    imagePath = "/cache/crop.jpg",
                    noteText = "Bài 1 · Nhân đôi ADN",
                    cards = listOf(
                        SaveSuggestedCardsUseCase.CardDraft("Đoạn Okazaki là gì?", "Đoạn ADN ngắn trên mạch chậm.", fromAi = true),
                        SaveSuggestedCardsUseCase.CardDraft("Câu tự thêm?", "Đáp tự thêm", fromAi = false),
                    ),
                ),
            )
        }
        verifyOrder {
            navigator.showSnackBar(any(), any())
            navigator.replaceAll(HomeRoute)
            navigator.to(DeckDetailRoute("d1"))
        }
    }

    @Test
    fun `luu khi khong tich the nao - khong lam gi`() = runTest {
        val vm = reviewingViewModel()
        vm.uiState.value.items.forEach { vm.onToggleSelected(it.id) }

        vm.onSave()
        advanceUntilIdle()

        coVerify(exactly = 0) { saveSuggestedCards.invoke(any()) }
    }

    @Test
    fun `luu loi do khong ghi duoc anh - bao snackbar, o lai man voi cac the con nguyen`() = runTest {
        coEvery { saveSuggestedCards.invoke(any()) } throws CaptureException(CaptureException.Kind.SAVE_FAILED)
        val vm = reviewingViewModel()

        vm.onSave()
        advanceUntilIdle()

        verify { navigator.showSnackBar(any(), any()) }
        verify(exactly = 0) { exceptionHandler.handle(any()) }
        verify(exactly = 0) { navigator.replaceAll(any()) }
        assertEquals(2, vm.uiState.value.items.size)
    }

    // ---- Báo cáo ----

    @Test
    fun `bao cao - gui kem noi dung cac the AI, khong kem the nguoi dung tu go`() = runTest {
        val vm = reviewingViewModel()
        vm.onCardAdded("Câu tự thêm?", "Đáp tự thêm")

        vm.effect.test {
            vm.onReportClick()
            val effect = awaitItem() as AiCardsEffect.ReportAiContent
            assertEquals(
                "Q: Nhân đôi ADN diễn ra ở pha nào?\nA: Pha S.\n\nQ: Đoạn Okazaki là gì?\nA: Đoạn ADN ngắn trên mạch chậm.",
                effect.cardsText,
            )
        }
    }
}
