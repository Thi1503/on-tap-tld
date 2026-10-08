package com.ledinhthi.ontaptld.feature.deck.data.repository

import com.ledinhthi.ontaptld.core.data.local.db.wrapLocal
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckDao
import com.ledinhthi.ontaptld.feature.deck.data.mapper.DeckEntityMapper
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DeckRepositoryImpl @Inject constructor(
    private val dao: DeckDao,
    private val mapper: DeckEntityMapper,
    private val clock: Clock,
) : DeckRepository {

    override fun observeDecks(): Flow<List<Deck>> =
        dao.observeAll().map { mapper.toDomainList(it) }

    override fun observeDeck(deckId: String): Flow<Deck?> =
        dao.observeById(deckId).map { mapper.toDomainOrNull(it) }

    override suspend fun upsert(deck: Deck) = wrapLocal { dao.upsert(mapper.toEntity(deck)) }

    override suspend fun delete(deckId: String) =
        wrapLocal { dao.softDeleteWithCards(deckId, clock.nowMillis()) }
}
