package com.ledinhthi.ontaptld.feature.review

import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import com.ledinhthi.ontaptld.core.domain.util.dueCutoffMillis
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.deck.domain.model.Note
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import com.ledinhthi.ontaptld.feature.deck.domain.repository.NoteRepository
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade
import com.ledinhthi.ontaptld.feature.review.domain.model.ExcerptLine
import com.ledinhthi.ontaptld.feature.review.domain.model.ReviewLog
import com.ledinhthi.ontaptld.feature.review.domain.model.toCardSource
import com.ledinhthi.ontaptld.feature.review.domain.repository.ReviewLogRepository
import com.ledinhthi.ontaptld.feature.review.domain.usecase.GetDueReviewCardsUseCase
import com.ledinhthi.ontaptld.feature.review.domain.usecase.ReviewFlashcardUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewUseCasesTest {

    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private val flashcards = mockk<FlashcardRepository>(relaxed = true)
    private val decks = mockk<DeckRepository>()
    private val notes = mockk<NoteRepository>()
    private val reviewLogs = mockk<ReviewLogRepository>(relaxed = true)
    private val clock = mockk<Clock> { every { nowMillis() } returns now }
    private val ids = mockk<IdGenerator> { every { newId() } returns "log-1" }

    private fun deck(id: String) = Deck(id, "Deck $id", "#4F46E5", createdAt = 1, updatedAt = 1)

    private fun card(id: String, deckId: String, interval: Int = 0, repetitions: Int = 0) = Flashcard(
        id = id,
        deckId = deckId,
        noteId = null,
        source = FlashcardSource.MANUAL,
        question = "Q$id",
        answer = "A$id",
        interval = interval,
        repetitions = repetitions,
        dueDate = now - 1,
        createdAt = 1,
        updatedAt = 1,
    )

    // ---- GetDueReviewCardsUseCase ----

    private fun getDueCards() = GetDueReviewCardsUseCase(flashcards, decks, notes, clock)

    private fun givenDue(vararg cards: Flashcard) {
        every { decks.observeDecks() } returns flowOf(listOf(deck("d1"), deck("d2")))
        // Phải hỏi database bằng mốc CUỐI NGÀY hôm nay, không phải "bây giờ".
        every { flashcards.observeDue(dueCutoffMillis(now)) } returns flowOf(cards.toList())
    }

    @Test
    fun `lay the den han cua moi bo - giu thu tu, gan kem bo the`() = runTest {
        givenDue(card("a", "d1"), card("b", "d2"), card("c", "d1"))

        val result = getDueCards()(null)

        assertEquals(listOf("a", "b", "c"), result.map { it.card.id })
        assertEquals(listOf("d1", "d2", "d1"), result.map { it.deck.id })
    }

    @Test
    fun `chi dinh bo the - chi lay the cua bo do`() = runTest {
        givenDue(card("a", "d1"), card("b", "d2"), card("c", "d1"))

        val result = getDueCards()("d2")

        assertEquals(listOf("b"), result.map { it.card.id })
    }

    @Test
    fun `the cua bo da xoa - bi bo qua`() = runTest {
        givenDue(card("a", "d1"), card("mo-coi", "da-xoa"))

        val result = getDueCards()(null)

        assertEquals(listOf("a"), result.map { it.card.id })
    }

    // ---- Nguồn của thẻ AI (đoạn trích ghi chú ở mặt đáp án) ----

    private val note = Note(
        id = "n1",
        deckId = "d1",
        imagePath = "/files/notes/n1.jpg",
        ocrText = "Bài 1 · Nhân đôi ADN\n\n– Pha S.\n– Bổ sung và bán bảo toàn.\n– Chiều 5'→3'.",
        createdAt = 5_000,
        updatedAt = 5_000,
    )

    private fun aiCard(id: String, sourceLine: Int?) =
        card(id, "d1").copy(noteId = "n1", source = FlashcardSource.AI, sourceLine = sourceLine)

    @Test
    fun `the AI con ghi chu - kem ngay chup va doan trich quanh dong nguon`() = runTest {
        givenDue(aiCard("a", sourceLine = 3))
        coEvery { notes.getById("n1") } returns note

        val source = getDueCards()(null).single().source

        assertEquals(5_000L, source?.capturedAt)
        // Dòng 3 tính theo dòng CÓ CHỮ (dòng trống không được đánh số): dòng trước, nguồn, sau.
        assertEquals(
            listOf(
                ExcerptLine("– Pha S.", isSource = false),
                ExcerptLine("– Bổ sung và bán bảo toàn.", isSource = true),
                ExcerptLine("– Chiều 5'→3'.", isSource = false),
            ),
            source?.excerpt,
        )
    }

    @Test
    fun `dong nguon la dong dau hoac dong cuoi - doan trich khong tran ra ngoai ghi chu`() {
        assertEquals(
            listOf(true, false),
            note.toCardSource(sourceLine = 1).excerpt.map { it.isSource },
        )
        assertEquals(
            listOf("– Bổ sung và bán bảo toàn.", "– Chiều 5'→3'."),
            note.toCardSource(sourceLine = 4).excerpt.map { it.text },
        )
    }

    @Test
    fun `khong biet dong nguon hoac so dong sai - khong co doan trich nhung van co ngay chup`() {
        assertTrue(note.toCardSource(sourceLine = null).excerpt.isEmpty())
        assertTrue(note.toCardSource(sourceLine = 9).excerpt.isEmpty())
        assertEquals(5_000L, note.toCardSource(sourceLine = null).capturedAt)
    }

    @Test
    fun `the thu cong hoac ghi chu da mat - khong co nguon`() = runTest {
        givenDue(card("thu-cong", "d1"), aiCard("mat-ghi-chu", sourceLine = 2))
        coEvery { notes.getById("n1") } returns null

        val result = getDueCards()(null)

        assertEquals(listOf(null, null), result.map { it.source })
    }

    @Test
    fun `nhieu the chung mot ghi chu - chi doc ghi chu mot lan`() = runTest {
        givenDue(aiCard("a", sourceLine = 2), aiCard("b", sourceLine = 3), aiCard("c", sourceLine = null))
        coEvery { notes.getById("n1") } returns note

        getDueCards()(null)

        coVerify(exactly = 1) { notes.getById("n1") }
    }

    // ---- ReviewFlashcardUseCase ----

    private fun reviewUseCase() = ReviewFlashcardUseCase(flashcards, reviewLogs, clock, ids)

    @Test
    fun `cham De cho the da on 1 lan - hen 6 ngay, luu the va ghi lich su`() = runTest {
        coEvery { flashcards.getById("a") } returns card("a", "d1", interval = 1, repetitions = 1)

        val updated = reviewUseCase()(ReviewFlashcardUseCase.Params("a", ReviewGrade.EASY))

        assertEquals(6, updated.interval)
        assertEquals(2, updated.repetitions)
        assertEquals(now + 6 * day, updated.dueDate)
        assertEquals(now, updated.lastReviewedAt)
        coVerify { flashcards.upsert(updated) }
        coVerify { reviewLogs.add(ReviewLog("log-1", "a", "d1", ReviewGrade.EASY, now)) }
    }

    @Test
    fun `cham Quen - quay ve tu dau, hen ngay mai`() = runTest {
        coEvery { flashcards.getById("a") } returns card("a", "d1", interval = 15, repetitions = 4)

        val updated = reviewUseCase()(ReviewFlashcardUseCase.Params("a", ReviewGrade.FORGOT))

        assertEquals(1, updated.interval)
        assertEquals(0, updated.repetitions)
        assertEquals(now + day, updated.dueDate)
    }

    @Test
    fun `the khong ton tai - nem CARD_NOT_FOUND, khong ghi gi`() = runTest {
        coEvery { flashcards.getById("x") } returns null

        val error = runCatching {
            reviewUseCase()(ReviewFlashcardUseCase.Params("x", ReviewGrade.EASY))
        }.exceptionOrNull()

        assertTrue(error is DeckException)
        assertEquals(DeckException.Kind.CARD_NOT_FOUND, (error as DeckException).kind)
        coVerify(exactly = 0) { reviewLogs.add(any()) }
    }
}
