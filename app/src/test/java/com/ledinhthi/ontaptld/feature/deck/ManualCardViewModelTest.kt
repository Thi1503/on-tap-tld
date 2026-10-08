package com.ledinhthi.ontaptld.feature.deck

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ledinhthi.ontaptld.core.MainDispatcherRule
import com.ledinhthi.ontaptld.core.exception.GlobalExceptionHandler
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CreateManualFlashcardUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.EditFlashcardUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.GetFlashcardUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDecksUseCase
import com.ledinhthi.ontaptld.feature.deck.presentation.manualcard.ManualCardEffect
import com.ledinhthi.ontaptld.feature.deck.presentation.manualcard.ManualCardViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManualCardViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val observeDecks = mockk<ObserveDecksUseCase>()
    private val getFlashcard = mockk<GetFlashcardUseCase>()
    private val createCard = mockk<CreateManualFlashcardUseCase>()
    private val editCard = mockk<EditFlashcardUseCase>()
    private val navigator = mockk<AppNavigator>(relaxed = true)
    private val exceptionHandler = mockk<GlobalExceptionHandler>(relaxed = true)
    private val strings = mockk<StringProvider>(relaxed = true)
    private val toolbox = ViewModelToolbox(navigator, exceptionHandler, strings)

    private val decks = listOf(
        Deck("d1", "IELTS", "#4F46E5", createdAt = 1, updatedAt = 1),
        Deck("d2", "Sinh học", "#0D9488", createdAt = 1, updatedAt = 1),
    )

    private fun card(id: String, deckId: String, source: FlashcardSource = FlashcardSource.MANUAL) = Flashcard(
        id = id,
        deckId = deckId,
        noteId = null,
        source = source,
        question = "Q cũ",
        answer = "A cũ",
        dueDate = 1,
        createdAt = 1,
        updatedAt = 1,
    )

    /** [cardId] = null: mở màn ở chế độ thêm thẻ vào bộ d1; có giá trị: chế độ sửa thẻ đó. */
    private fun viewModel(cardId: String? = null): ManualCardViewModel {
        every { observeDecks.invoke() } returns flowOf(decks)
        return ManualCardViewModel(
            toolbox = toolbox,
            savedState = SavedStateHandle(mapOf("deckId" to "d1", "cardId" to cardId)),
            observeDecks = observeDecks,
            getFlashcard = getFlashcard,
            createManualFlashcard = createCard,
            editFlashcard = editCard,
        ).apply { isTestMode = true }
    }

    private fun ManualCardViewModel.type(question: String, answer: String) {
        onQuestionChange(question)
        onAnswerChange(answer)
    }

    @Test
    fun `mo man them the - chon san bo the tu route, chua luu duoc khi con o trong`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isEditing)
        assertEquals("d1", state.selectedDeck?.id)
        assertFalse(state.canSave)

        vm.type("Q", "   ")
        assertFalse(vm.uiState.value.canSave)
        vm.onAnswerChange("A")
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `luu khi dang tick them the tiep - o lai man, xoa trang o nhap, dem so the da luu`() = runTest {
        coEvery { createCard.invoke(any()) } returns card("moi", "d1")
        val vm = viewModel()
        advanceUntilIdle()
        vm.type("Q1", "A1")

        vm.effect.test {
            vm.onSave()
            assertEquals(ManualCardEffect.FocusQuestion, awaitItem())
        }

        val state = vm.uiState.value
        assertEquals("", state.question)
        assertEquals("", state.answer)
        assertEquals(1, state.savedCount)
        coVerify { createCard.invoke(CreateManualFlashcardUseCase.Params("d1", "Q1", "A1")) }
        verify(exactly = 0) { navigator.back() }
    }

    @Test
    fun `luu khi bo tick them the tiep - bao da them roi dong man`() = runTest {
        coEvery { createCard.invoke(any()) } returns card("moi", "d1")
        val vm = viewModel()
        advanceUntilIdle()
        vm.onKeepAddingChange(false)
        vm.type("Q1", "A1")

        vm.onSave()
        advanceUntilIdle()

        verify { navigator.showSnackBar(any(), any()) }
        verify { navigator.back() }
    }

    @Test
    fun `doi bo the roi luu - the vao bo vua chon`() = runTest {
        coEvery { createCard.invoke(any()) } returns card("moi", "d2")
        val vm = viewModel()
        advanceUntilIdle()
        vm.onDeckSelected("d2")
        vm.type("Q1", "A1")

        vm.onSave()
        advanceUntilIdle()

        coVerify { createCard.invoke(CreateManualFlashcardUseCase.Params("d2", "Q1", "A1")) }
    }

    @Test
    fun `mo man sua the - nap noi dung, nguon va bo the cua the do`() = runTest {
        coEvery { getFlashcard.invoke("c1") } returns card("c1", "d2", FlashcardSource.AI)

        val vm = viewModel(cardId = "c1")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isEditing)
        assertEquals("Q cũ", state.question)
        assertEquals("A cũ", state.answer)
        assertEquals(FlashcardSource.AI, state.source)
        assertEquals("d2", state.selectedDeck?.id)
    }

    @Test
    fun `luu khi dang sua - goi EditFlashcard chu khong tao the moi, roi dong man`() = runTest {
        coEvery { getFlashcard.invoke("c1") } returns card("c1", "d1")
        coEvery { editCard.invoke(any()) } returns card("c1", "d1")
        val vm = viewModel(cardId = "c1")
        advanceUntilIdle()
        vm.onQuestionChange("Q mới")

        vm.onSave()
        advanceUntilIdle()

        coVerify { editCard.invoke(EditFlashcardUseCase.Params("c1", "Q mới", "A cũ")) }
        coVerify(exactly = 0) { createCard.invoke(any()) }
        verify { navigator.back() }
    }

    @Test
    fun `mo man sua nhung the khong con - bao loi va dong man`() = runTest {
        coEvery { getFlashcard.invoke("c1") } returns null

        viewModel(cardId = "c1")
        advanceUntilIdle()

        verify { navigator.showSnackBar(any(), any()) }
        verify { navigator.back() }
    }

    @Test
    fun `them the - co noi dung chua luu khi da go, het sau khi luu va o lai man`() = runTest {
        coEvery { createCard.invoke(any()) } returns card("moi", "d1")
        val vm = viewModel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.hasUnsavedChanges)

        vm.onQuestionChange("   ") // chỉ có dấu cách thì chưa tính là đã gõ
        assertFalse(vm.uiState.value.hasUnsavedChanges)
        vm.type("Q1", "A1")
        assertTrue(vm.uiState.value.hasUnsavedChanges)

        vm.onSave()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `sua the - chi tinh la co thay doi khi noi dung khac luc nap`() = runTest {
        coEvery { getFlashcard.invoke("c1") } returns card("c1", "d1")
        val vm = viewModel(cardId = "c1")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.hasUnsavedChanges)

        vm.onAnswerChange("A mới")
        assertTrue(vm.uiState.value.hasUnsavedChanges)

        vm.onAnswerChange("A cũ") // gõ lại đúng nội dung cũ = không còn gì để mất
        assertFalse(vm.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `luu the rong - hien snackbar, khong goi ExceptionHandler, khong dong man`() = runTest {
        coEvery { createCard.invoke(any()) } throws DeckException(DeckException.Kind.BLANK_CARD)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onSave()
        advanceUntilIdle()

        verify { navigator.showSnackBar(any(), any()) }
        verify(exactly = 0) { exceptionHandler.handle(any()) }
        verify(exactly = 0) { navigator.back() }
    }
}
