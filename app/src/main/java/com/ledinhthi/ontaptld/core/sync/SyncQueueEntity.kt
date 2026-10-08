package com.ledinhthi.ontaptld.core.sync

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SyncEntityType { DECK, NOTE, FLASHCARD }

enum class SyncAction { CREATE, UPDATE, DELETE }

/** Tên collection trên Firestore của từng loại dữ liệu (docs_tld Mục 7.1). */
val SyncEntityType.collectionName: String
    get() = when (this) {
        SyncEntityType.DECK -> "decks"
        SyncEntityType.NOTE -> "notes"
        SyncEntityType.FLASHCARD -> "flashcards"
    }

/**
 * Hàng đợi lệnh chờ đẩy lên Firestore (docs_tld Mục 6.4). Ghi vào đây: [SyncQueueRecorder];
 * đọc ra để đẩy lên: `feature/sync`.
 *
 * Mỗi dòng dữ liệu chỉ có MỘT lệnh chờ: [id] của lệnh lấy luôn [entityId], nên ghi lệnh mới là
 * đè lệnh cũ (sửa 1 thẻ nhiều lần lúc offline chỉ giữ 1 lệnh). Lệnh không chép nội dung của dòng
 * dữ liệu — lúc đẩy mới đọc dòng đó ra, nên thứ lên đám mây luôn là bản mới nhất.
 */
@Entity(tableName = "sync_queue", indices = [Index("entityId")])
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val entityType: SyncEntityType,
    val entityId: String,
    val action: SyncAction,
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null,
)
