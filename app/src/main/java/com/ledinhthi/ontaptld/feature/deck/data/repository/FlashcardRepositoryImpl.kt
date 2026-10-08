package com.ledinhthi.ontaptld.feature.deck.data.repository

import com.ledinhthi.ontaptld.core.data.local.db.wrapLocal
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardDao
import com.ledinhthi.ontaptld.feature.deck.data.mapper.FlashcardEntityMapper
import com.ledinhthi.ontaptld.feature.deck.domain.model.DeckCardStats
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FlashcardRepositoryImpl @Inject constructor(
    private val dao: FlashcardDao,
    private val mapper: FlashcardEntityMapper,
    private val clock: Clock,
) : FlashcardRepository {

    override fun observeByDeck(deckId: String): Flow<List<Flashcard>> =
        dao.observeByDeck(deckId).map { mapper.toDomainList(it) }

    override fun observeDue(nowMillis: Long): Flow<List<Flashcard>> =
        dao.observeDue(nowMillis).map { mapper.toDomainList(it) }

    override fun observeDueCount(nowMillis: Long): Flow<Int> = dao.observeDueCount(nowMillis)

    override fun observeDeckStats(dueBeforeMillis: Long): Flow<List<DeckCardStats>> =
        dao.observeDeckStats(dueBeforeMillis).map { rows ->
            rows.map { DeckCardStats(it.deckId, it.cardCount, it.dueCount) }
        }

    override suspend fun getById(id: String): Flashcard? =
        wrapLocal { dao.getById(id)?.let(mapper::toDomain) }

    override suspend fun upsert(card: Flashcard) = wrapLocal { dao.upsert(mapper.toEntity(card)) }

    override suspend fun upsertAll(cards: List<Flashcard>) =
        wrapLocal { dao.upsertAll(mapper.toEntityList(cards)) }

    override suspend fun delete(id: String) = wrapLocal { dao.softDelete(id, clock.nowMillis()) }
}
