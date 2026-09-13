package com.ledinhthi.ontaptld.feature.deck.domain.repository

import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import kotlinx.coroutines.flow.Flow

interface DeckRepository {
    fun observeDecks(): Flow<List<Deck>>
    fun observeDeck(deckId: String): Flow<Deck?>
    suspend fun upsert(deck: Deck)
    suspend fun delete(deckId: String)
}
