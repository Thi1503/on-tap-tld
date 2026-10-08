package com.ledinhthi.ontaptld.feature.deck.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.ledinhthi.ontaptld.core.data.local.db.BaseDao
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao : BaseDao<DeckEntity> {
    @Query("SELECT * FROM decks WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :id AND isDeleted = 0")
    fun observeById(id: String): Flow<DeckEntity?>

    @Query("UPDATE decks SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE flashcards SET isDeleted = 1, updatedAt = :now WHERE deckId = :deckId AND isDeleted = 0")
    suspend fun softDeleteCardsOf(deckId: String, now: Long)

    /**
     * Xoá bộ thẻ KÈM các thẻ bên trong. Nếu chỉ đánh dấu xoá bộ thẻ thì thẻ của nó thành "mồ côi":
     * không còn thấy ở đâu nhưng vẫn bị đếm/lấy ra khi ôn tập.
     * `@Transaction`: hai lệnh UPDATE hoặc cùng thành công, hoặc cùng không có gì thay đổi.
     */
    @Transaction
    suspend fun softDeleteWithCards(id: String, now: Long) {
        softDelete(id, now)
        softDeleteCardsOf(id, now)
    }
}
