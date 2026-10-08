package com.ledinhthi.ontaptld.feature.deck.domain.repository

import com.ledinhthi.ontaptld.feature.deck.domain.model.DeckCardStats
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import kotlinx.coroutines.flow.Flow

interface FlashcardRepository {
    fun observeByDeck(deckId: String): Flow<List<Flashcard>>
    fun observeDue(nowMillis: Long): Flow<List<Flashcard>> // dùng cho Review + Widget
    fun observeDueCount(nowMillis: Long): Flow<Int>

    /** Tổng số thẻ và số thẻ có `dueDate <= dueBeforeMillis`, gom theo từng bộ thẻ. */
    fun observeDeckStats(dueBeforeMillis: Long): Flow<List<DeckCardStats>>
    suspend fun getById(id: String): Flashcard?
    suspend fun upsert(card: Flashcard)
    suspend fun upsertAll(cards: List<Flashcard>)
    suspend fun delete(id: String)
}
