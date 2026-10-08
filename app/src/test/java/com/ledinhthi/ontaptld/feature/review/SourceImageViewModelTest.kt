package com.ledinhthi.ontaptld.feature.review

import androidx.lifecycle.SavedStateHandle
import com.ledinhthi.ontaptld.core.MainDispatcherRule
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.GlobalExceptionHandler
import com.ledinhthi.ontaptld.core.exception.LocalErrorKind
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.deck.domain.model.Note
import com.ledinhthi.ontaptld.feature.deck.domain.model.SourceBox
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import com.ledinhthi.ontaptld.feature.deck.domain.repository.NoteRepository
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.GetCardSourceImageUseCase
import com.ledinhthi.ontaptld.feature.review.presentation.sourceimage.SourceImageViewModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SourceImageViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val flashcards = mockk<FlashcardRepository>()
    private val notes = mockk<NoteRepository>()
    private val navigator = mockk<AppNavigator>(relaxed = true)
    private val exceptionHandler = mockk<GlobalExceptionHandler>(relaxed = true)
    private val strings = mockk<StringProvider>(relaxed = true)
    private val toolbox = ViewModelToolbox(navigator, exceptionHandler, strings)

    private val box = SourceBox(left = 0.1f, top = 0.4f, right = 0.9f, bottom = 0.5f)

    private fun card(noteId: String?, sourceBox: SourceBox? = null) = Flashcard(
        id = "c1",
        deckId = "d1",
        noteId = noteId,
        source = if (noteId == null) FlashcardSource.MANUAL else FlashcardSource.AI,
        question = "Q",
        answer = "A",
        sourceBox = sourceBox,
        dueDate = 1,
        createdAt = 1,
        updatedAt = 1,
    )

    private val note = Note("n1", "d1", "/files/notes/n1.jpg", "Bài 1", createdAt = 1, updatedAt = 1)

    // Dùng use case thật trên hai kho dữ liệu giả: kiểm luôn cả đường đi thẻ → ghi chú → ảnh.
    private fun viewModel() = SourceImageViewModel(
        toolbox = toolbox,
        savedState = SavedStateHandle(mapOf("cardId" to "c1")),
        getCardSourceImage = GetCardSourceImageUseCase(flashcards, notes),
    ).apply { isTestMode = true }

    @Test
    fun `the AI co vung nguon - co duong dan anh va vung can to sang`() = runTest {
        coEvery { flashcards.getById("c1") } returns card(noteId = "n1", sourceBox = box)
        coEvery { notes.getById("n1") } returns note

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("/files/notes/n1.jpg", state.imagePath)
        assertEquals(box, state.box)
    }

    @Test
    fun `the khong co vung nguon - van xem duoc anh, khong to sang`() = runTest {
        coEvery { flashcards.getById("c1") } returns card(noteId = "n1", sourceBox = null)
        coEvery { notes.getById("n1") } returns note

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals("/files/notes/n1.jpg", vm.uiState.value.imagePath)
        assertNull(vm.uiState.value.box)
    }

    @Test
    fun `the thu cong, the da xoa hoac ghi chu da mat - khong co anh`() = runTest {
        coEvery { flashcards.getById("c1") } returnsMany listOf(card(noteId = null), null, card(noteId = "n1"))
        coEvery { notes.getById("n1") } returns null

        repeat(3) {
            val vm = viewModel()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.imagePath)
        }
    }

    @Test
    fun `doc database loi - bao ngay trong man, khong bat thong bao chung`() = runTest {
        coEvery { flashcards.getById("c1") } throws AppException.LocalException(LocalErrorKind.DISK_FULL)

        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.imagePath)
        verify(exactly = 0) { exceptionHandler.handle(any()) }
    }

    @Test
    fun `dong man - lui ve man truoc`() = runTest {
        coEvery { flashcards.getById("c1") } returns null
        val vm = viewModel()

        vm.onClose()

        verify { navigator.back() }
    }
}
