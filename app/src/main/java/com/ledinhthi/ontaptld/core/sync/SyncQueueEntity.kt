package com.ledinhthi.ontaptld.core.sync

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SyncEntityType { DECK, NOTE, FLASHCARD }

enum class SyncAction { CREATE, UPDATE, DELETE }

/**
 * Hàng đợi lệnh chờ đẩy lên Firestore (docs_tld Mục 6.4). Bảng có mặt từ bây giờ để schema
 * không phải đổi khi làm đồng bộ; phần Push/Pull dùng nó nằm ở bước 6 của
 * docs/KE_HOACH_PHAT_TRIEN.md.
 *
 * Quy tắc khi ghi: trước khi thêm, tìm lệnh đang chờ của cùng [entityId] — nếu đã có thì cập
 * nhật lệnh đó thay vì thêm dòng mới (sửa 1 thẻ nhiều lần lúc offline chỉ giữ 1 lệnh).
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
