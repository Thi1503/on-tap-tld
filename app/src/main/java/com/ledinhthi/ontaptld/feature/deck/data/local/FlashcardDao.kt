package com.ledinhthi.ontaptld.feature.deck.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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

    // Một câu lệnh đếm cho TẤT CẢ bộ thẻ cùng lúc (thay vì mỗi bộ một câu):
    //   COUNT(*)            -> tổng số thẻ của bộ
    //   SUM(CASE WHEN ...)  -> cộng 1 cho mỗi thẻ đã đến hạn, 0 cho thẻ chưa đến hạn
    // Tên cột sau `AS` phải trùng tên field của DeckCardStatsRow để Room tự ghép.
    @Query(
        "SELECT deckId, COUNT(*) AS cardCount, " +
            "SUM(CASE WHEN dueDate <= :dueBefore THEN 1 ELSE 0 END) AS dueCount " +
            "FROM flashcards WHERE isDeleted = 0 GROUP BY deckId",
    )
    fun observeDeckStats(dueBefore: Long): Flow<List<DeckCardStatsRow>>

    @Query("UPDATE flashcards SET isDeleted = 1, synced = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT COUNT(*) FROM flashcards WHERE noteId = :noteId AND isDeleted = 0")
    suspend fun countByNote(noteId: String): Int

    @Query("SELECT imagePath FROM notes WHERE id = :noteId AND isDeleted = 0")
    suspend fun noteImagePath(noteId: String): String?

    @Query("UPDATE notes SET isDeleted = 1, synced = 0, updatedAt = :now WHERE id = :noteId")
    suspend fun softDeleteNote(noteId: String, now: Long)

    /**
     * Xoá một thẻ. Một ghi chú (ảnh chụp) sinh ra NHIỀU thẻ AI, nên ghi chú chỉ bị xoá theo khi
     * đây là thẻ cuối cùng còn dùng nó — các thẻ còn lại vẫn phải xem được ảnh nguồn.
     *
     * Trả về đường dẫn ảnh của ghi chú vừa bị xoá theo (để nơi gọi xoá file), hoặc null nếu
     * không có ghi chú nào bị xoá.
     */
    @Transaction
    suspend fun softDeleteAndReleaseNote(id: String, now: Long): String? {
        val noteId = getById(id)?.noteId
        softDelete(id, now)
        if (noteId == null || countByNote(noteId) > 0) return null
        val imagePath = noteImagePath(noteId)
        softDeleteNote(noteId, now)
        return imagePath
    }
}

/** Kết quả chiếu của [FlashcardDao.observeDeckStats] — không phải bảng. */
data class DeckCardStatsRow(
    val deckId: String,
    val cardCount: Int,
    val dueCount: Int,
)
