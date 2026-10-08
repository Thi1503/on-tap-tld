package com.ledinhthi.ontaptld.feature.deck

import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.dueCutoffMillis
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.DeckCardStats
import com.ledinhthi.ontaptld.feature.deck.domain.model.DeckSummary
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDeckSummariesUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveDeckSummariesUseCaseTest {

    private val now = 1_760_000_000_000L
    private val deckRepository = mockk<DeckRepository>()
    private val flashcardRepository = mockk<FlashcardRepository>()
    private val clock = mockk<Clock> { every { nowMillis() } returns now }
    private val useCase = ObserveDeckSummariesUseCase(deckRepository, flashcardRepository, clock)

    private fun deck(id: String) = Deck(id, "Deck $id", "#4F46E5", createdAt = 1, updatedAt = 1)

    @Test
    fun `ghep so lieu vao dung bo the va giu thu tu cua danh sach bo the`() = runTest {
        every { deckRepository.observeDecks() } returns flowOf(listOf(deck("b"), deck("a")))
        every { flashcardRepository.observeDeckStats(any()) } returns flowOf(
            listOf(DeckCardStats("a", cardCount = 10, dueCount = 3), DeckCardStats("b", cardCount = 5, dueCount = 0)),
        )

        val result = useCase().first()

        assertEquals(
            listOf(
                DeckSummary(deck("b"), cardCount = 5, dueCount = 0),
                DeckSummary(deck("a"), cardCount = 10, dueCount = 3),
            ),
            result,
        )
    }

    @Test
    fun `bo the chua co the nao - van co mat voi so lieu bang 0`() = runTest {
        every { deckRepository.observeDecks() } returns flowOf(listOf(deck("a")))
        every { flashcardRepository.observeDeckStats(any()) } returns flowOf(emptyList())

        assertEquals(listOf(DeckSummary(deck("a"), cardCount = 0, dueCount = 0)), useCase().first())
    }

    @Test
    fun `the cua bo da xoa - khong lam xuat hien dong nao`() = runTest {
        every { deckRepository.observeDecks() } returns flowOf(listOf(deck("a")))
        every { flashcardRepository.observeDeckStats(any()) } returns flowOf(
            listOf(DeckCardStats("da-xoa", cardCount = 9, dueCount = 9)),
        )

        assertEquals(listOf(DeckSummary(deck("a"))), useCase().first())
    }

    @Test
    fun `dem the den han theo moc cuoi ngay hom nay chu khong phai luc nay`() = runTest {
        every { deckRepository.observeDecks() } returns flowOf(emptyList())
        every { flashcardRepository.observeDeckStats(any()) } returns flowOf(emptyList())

        useCase().first()

        verify { flashcardRepository.observeDeckStats(dueCutoffMillis(now)) }
    }
}
