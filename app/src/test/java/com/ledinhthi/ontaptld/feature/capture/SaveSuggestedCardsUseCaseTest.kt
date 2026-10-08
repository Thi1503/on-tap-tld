package com.ledinhthi.ontaptld.feature.capture

import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.LocalErrorKind
import com.ledinhthi.ontaptld.feature.capture.domain.exception.CaptureException
import com.ledinhthi.ontaptld.feature.capture.domain.repository.CaptureImageRepository
import com.ledinhthi.ontaptld.feature.capture.domain.usecase.SaveSuggestedCardsUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.deck.domain.model.Note
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import com.ledinhthi.ontaptld.feature.deck.domain.repository.NoteRepository
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CreateFlashcardsFromNoteUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class SaveSuggestedCardsUseCaseTest {

    private val imageRepository = mockk<CaptureImageRepository>(relaxed = true)
    private val noteRepository = mockk<NoteRepository>(relaxed = true)
    private val flashcardRepository = mockk<FlashcardRepository>(relaxed = true)
    private val clock = mockk<Clock> { every { nowMillis() } returns 1_000L }

    // Id đầu tiên được xin là id của ghi chú; các id sau là của từng thẻ.
    private val ids = mockk<IdGenerator> { every { newId() } returnsMany listOf("note-1", "card-1", "card-2") }

    private val save = SaveSuggestedCardsUseCase(
        imageRepository = imageRepository,
        noteRepository = noteRepository,
        // Use case của deck đủ đơn giản để dùng bản thật, chỉ thay kho dữ liệu bên dưới nó.
        createFlashcardsFromNote = CreateFlashcardsFromNoteUseCase(flashcardRepository, clock, ids),
        clock = clock,
        ids = ids,
    )

    private val params = SaveSuggestedCardsUseCase.Params(
        deckId = "d1",
        imagePath = "/cache/crop.jpg",
        noteText = "Bài 1 · Nhân đôi ADN",
        cards = listOf(
            SaveSuggestedCardsUseCase.CardDraft(" Câu AI? ", "Đáp AI", fromAi = true),
            SaveSuggestedCardsUseCase.CardDraft("Câu tự gõ?", "Đáp tự gõ", fromAi = false),
        ),
    )

    @Test
    fun `luu - giu anh, ghi ghi chu roi ghi the theo dung thu tu`() = runTest {
        coEvery { imageRepository.keepNoteImage("/cache/crop.jpg", "note-1") } returns "/files/notes/note-1.jpg"
        val cards = slot<List<Flashcard>>()
        coEvery { flashcardRepository.upsertAll(capture(cards)) } returns Unit

        save(params)

        coVerifyOrder {
            imageRepository.keepNoteImage("/cache/crop.jpg", "note-1")
            noteRepository.upsert(
                Note(
                    id = "note-1",
                    deckId = "d1",
                    imagePath = "/files/notes/note-1.jpg",
                    ocrText = "Bài 1 · Nhân đôi ADN",
                    createdAt = 1_000L,
                    updatedAt = 1_000L,
                ),
            )
            flashcardRepository.upsertAll(any())
        }
        val (aiCard, manualCard) = cards.captured
        // Thẻ AI gắn với ghi chú (để xem lại ảnh nguồn); thẻ tự gõ thì không.
        assertEquals(FlashcardSource.AI, aiCard.source)
        assertEquals("note-1", aiCard.noteId)
        assertEquals("Câu AI?", aiCard.question)
        assertEquals(1_000L, aiCard.dueDate) // thẻ mới ôn được ngay hôm nay
        assertEquals(FlashcardSource.MANUAL, manualCard.source)
        assertNull(manualCard.noteId)
        assertEquals("d1", manualCard.deckId)
    }

    @Test
    fun `ghi the hong - don ghi chu va anh da giu, roi bao dung loi goc`() = runTest {
        coEvery { imageRepository.keepNoteImage(any(), any()) } returns "/files/notes/note-1.jpg"
        val failure = AppException.LocalException(LocalErrorKind.DISK_FULL)
        coEvery { flashcardRepository.upsertAll(any()) } throws failure

        try {
            save(params)
            fail("Phải ném lỗi")
        } catch (e: AppException.LocalException) {
            assertEquals(LocalErrorKind.DISK_FULL, e.kind)
        }

        coVerify { noteRepository.delete("note-1") }
        coVerify { imageRepository.deleteNoteImage("/files/notes/note-1.jpg") }
    }

    @Test
    fun `khong giu duoc anh - dung ngay, chua ghi gi vao database`() = runTest {
        coEvery { imageRepository.keepNoteImage(any(), any()) } throws
            CaptureException(CaptureException.Kind.SAVE_FAILED)

        try {
            save(params)
            fail("Phải ném lỗi")
        } catch (e: CaptureException) {
            assertEquals(CaptureException.Kind.SAVE_FAILED, e.kind)
        }

        coVerify(exactly = 0) { noteRepository.upsert(any()) }
        coVerify(exactly = 0) { flashcardRepository.upsertAll(any()) }
    }
}
