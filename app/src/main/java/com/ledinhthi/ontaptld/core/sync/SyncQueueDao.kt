package com.ledinhthi.ontaptld.core.sync

import androidx.room.Dao
import androidx.room.Query
import com.ledinhthi.ontaptld.core.data.local.db.BaseDao

@Dao
interface SyncQueueDao : BaseDao<SyncQueueEntity> {
    /** Đọc tuần tự theo thứ tự tạo — Push xử lý lệnh cũ trước. */
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAll(): List<SyncQueueEntity>

    /**
     * Lô lệnh kế tiếp cần đẩy, cũ trước. Bỏ qua lệnh đã hỏng quá [maxRetry] lần (để một lệnh hỏng
     * không chặn cả hàng) và các lệnh trong [excludedIds] (vừa hỏng ngay trong lượt này).
     */
    @Query(
        "SELECT * FROM sync_queue WHERE retryCount < :maxRetry AND id NOT IN (:excludedIds) " +
            "ORDER BY createdAt ASC LIMIT :limit",
    )
    suspend fun nextBatch(limit: Int, maxRetry: Int, excludedIds: List<String>): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE entityId = :entityId LIMIT 1")
    suspend fun findByEntityId(entityId: String): SyncQueueEntity?

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String?)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sync_queue")
    suspend fun clear()
}
