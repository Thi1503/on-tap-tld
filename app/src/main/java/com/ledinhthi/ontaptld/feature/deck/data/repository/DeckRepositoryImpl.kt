package com.ledinhthi.ontaptld.feature.deck.data.repository

import com.ledinhthi.ontaptld.core.data.local.db.wrapLocal
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.sync.SyncEntityType
import com.ledinhthi.ontaptld.core.sync.SyncQueueRecorder
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckDao
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteImageStore
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
    private val images: NoteImageStore,
    private val sync: SyncQueueRecorder,
) : DeckRepository {

    override fun observeDecks(): Flow<List<Deck>> =
        dao.observeAll().map { mapper.toDomainList(it) }

    override fun observeDeck(deckId: String): Flow<Deck?> =
        dao.observeById(deckId).map { mapper.toDomainOrNull(it) }

    override suspend fun upsert(deck: Deck) = wrapLocal {
        dao.upsert(mapper.toEntity(deck))
        // Mỗi lần ghi đều để lại một lệnh trong hàng đợi đồng bộ (xem SyncQueueRecorder).
        sync.recordChanged(SyncEntityType.DECK, listOf(deck.id))
    }

    override suspend fun delete(deckId: String) {
        val imagePaths = wrapLocal {
            dao.softDeleteWithContents(deckId, clock.nowMillis()).also {
                sync.recordChanged(SyncEntityType.DECK, listOf(deckId))
                sync.recordCascades() // các thẻ và ghi chú bị xoá theo
            }
        }
        // Xoá file SAU khi database đã ghi xong: nếu database lỗi thì ảnh còn nguyên, không có
        // chuyện thẻ vẫn còn mà ảnh nguồn đã mất.
        images.delete(imagePaths)
    }
}
