package com.ledinhthi.ontaptld.feature.deck.data.local

import androidx.room.Dao
import androidx.room.Query
import com.ledinhthi.ontaptld.core.data.local.db.BaseDao
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao : BaseDao<FlashcardEntity> {
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun observeByDeck(deckId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun getById(id: String): FlashcardEntity?

    @Query("SELECT * FROM flashcards WHERE isDeleted = 0 AND dueDate <= :now ORDER BY dueDate ASC")
    fun observeDue(now: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT COUNT(*) FROM flashcards WHERE isDeleted = 0 AND dueDate <= :now")
    fun observeDueCount(now: Long): Flow<Int>

    @Query("UPDATE flashcards SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}
