package com.ledinhthi.ontaptld.feature.deck.data.repository

import com.ledinhthi.ontaptld.core.data.local.db.wrapLocal
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.sync.SyncEntityType
import com.ledinhthi.ontaptld.core.sync.SyncQueueRecorder
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteDao
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteImageStore
import com.ledinhthi.ontaptld.feature.deck.data.mapper.NoteEntityMapper
import com.ledinhthi.ontaptld.feature.deck.domain.model.Note
import com.ledinhthi.ontaptld.feature.deck.domain.repository.NoteRepository
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val dao: NoteDao,
    private val mapper: NoteEntityMapper,
    private val clock: Clock,
    private val images: NoteImageStore,
    private val sync: SyncQueueRecorder,
) : NoteRepository {

    override suspend fun getById(id: String): Note? =
        wrapLocal { dao.getById(id)?.let(mapper::toDomain) }

    override suspend fun upsert(note: Note) = wrapLocal {
        dao.upsert(mapper.toEntity(note))
        sync.recordChanged(SyncEntityType.NOTE, listOf(note.id))
    }

    override suspend fun delete(id: String) = wrapLocal {
        dao.softDelete(id, clock.nowMillis())
        sync.recordChanged(SyncEntityType.NOTE, listOf(id))
    }

    override suspend fun cleanUpOrphans() {
        val now = clock.nowMillis()
        // Thứ gì mới hơn mốc này thì chưa đụng tới: có thể một lượt lưu thẻ AI đang chạy dở (ảnh
        // đã chép, ghi chú đã ghi, thẻ chưa ghi xong).
        val settledBefore = now - ORPHAN_GRACE_MILLIS
        wrapLocal {
            val deleted = dao.softDeleteOrphans(now, createdBefore = settledBefore)
            if (deleted > 0) sync.recordCascades()
        }
        // Đọc danh sách ảnh còn dùng SAU bước trên, để ảnh của ghi chú vừa bị xoá cũng được dọn.
        val livePaths = wrapLocal { dao.liveImagePaths() }
        images.deleteUnreferenced(keepPaths = livePaths, modifiedBefore = settledBefore)
    }

    private companion object {
        /** Lưu thẻ AI chỉ mất vài giây; một phút là dư để không đụng nhầm lượt đang lưu. */
        const val ORPHAN_GRACE_MILLIS = 60_000L
    }
}
