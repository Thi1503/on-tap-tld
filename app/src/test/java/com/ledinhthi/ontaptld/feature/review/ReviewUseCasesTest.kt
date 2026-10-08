package com.ledinhthi.ontaptld.feature.review

import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import com.ledinhthi.ontaptld.core.domain.util.dueCutoffMillis
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade
import com.ledinhthi.ontaptld.feature.review.domain.model.ReviewLog
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

    private fun givenDue(vararg cards: Flashcard) {
        every { decks.observeDecks() } returns flowOf(listOf(deck("d1"), deck("d2")))
        // Phải hỏi database bằng mốc CUỐI NGÀY hôm nay, không phải "bây giờ".
        every { flashcards.observeDue(dueCutoffMillis(now)) } returns flowOf(cards.toList())
    }

    @Test
    fun `lay the den han cua moi bo - giu thu tu, gan kem bo the`() = runTest {
        givenDue(card("a", "d1"), card("b", "d2"), card("c", "d1"))

        val result = GetDueReviewCardsUseCase(flashcards, decks, clock)(null)

        assertEquals(listOf("a", "b", "c"), result.map { it.card.id })
        assertEquals(listOf("d1", "d2", "d1"), result.map { it.deck.id })
    }

    @Test
    fun `chi dinh bo the - chi lay the cua bo do`() = runTest {
        givenDue(card("a", "d1"), card("b", "d2"), card("c", "d1"))

        val result = GetDueReviewCardsUseCase(flashcards, decks, clock)("d2")

        assertEquals(listOf("b"), result.map { it.card.id })
    }

    @Test
    fun `the cua bo da xoa - bi bo qua`() = runTest {
        givenDue(card("a", "d1"), card("mo-coi", "da-xoa"))

        val result = GetDueReviewCardsUseCase(flashcards, decks, clock)(null)

        assertEquals(listOf("a"), result.map { it.card.id })
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
