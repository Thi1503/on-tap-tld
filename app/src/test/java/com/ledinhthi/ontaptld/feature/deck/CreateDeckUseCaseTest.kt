package com.ledinhthi.ontaptld.feature.deck

import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CreateDeckUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateDeckUseCaseTest {

    private val repository = mockk<DeckRepository>(relaxed = true)
    private val clock = mockk<Clock> { every { nowMillis() } returns 1_000L }
    private val ids = mockk<IdGenerator> { every { newId() } returns "deck-1" }
    private val useCase = CreateDeckUseCase(repository, clock, ids)

    @Test
    fun `ten rong - nem DeckException BLANK_NAME`() = runTest {
        val error = runCatching { useCase(CreateDeckUseCase.Params("   ", "#fff")) }.exceptionOrNull()
        assertTrue(error is DeckException)
        assertEquals(DeckException.Kind.BLANK_NAME, (error as DeckException).kind)
    }

    @Test
    fun `ten hop le - tao Deck va goi repository upsert`() = runTest {
        val deck = useCase(CreateDeckUseCase.Params("  Lich su  ", "#4F7CFF"))

        assertEquals(Deck("deck-1", "Lich su", "#4F7CFF", 1_000L, 1_000L), deck)
        coVerify(exactly = 1) { repository.upsert(deck) }
    }
}
