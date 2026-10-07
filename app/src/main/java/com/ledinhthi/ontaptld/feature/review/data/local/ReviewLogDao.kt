package com.ledinhthi.ontaptld.feature.review.data.local

import androidx.room.Dao
import androidx.room.Query
import com.ledinhthi.ontaptld.core.data.local.db.BaseDao
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewLogDao : BaseDao<ReviewLogEntity> {
    @Query("SELECT * FROM review_logs WHERE reviewedAt >= :fromMillis ORDER BY reviewedAt ASC")
    fun observeSince(fromMillis: Long): Flow<List<ReviewLogEntity>>

    @Query("DELETE FROM review_logs")
    suspend fun clear()
}
