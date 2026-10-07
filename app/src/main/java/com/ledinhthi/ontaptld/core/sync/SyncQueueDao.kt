package com.ledinhthi.ontaptld.core.sync

import androidx.room.Dao
import androidx.room.Query
import com.ledinhthi.ontaptld.core.data.local.db.BaseDao

@Dao
interface SyncQueueDao : BaseDao<SyncQueueEntity> {
    /** Đọc tuần tự theo thứ tự tạo — Push xử lý lệnh cũ trước. */
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAll(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE entityId = :entityId LIMIT 1")
    suspend fun findByEntityId(entityId: String): SyncQueueEntity?

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sync_queue")
    suspend fun clear()
}
