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

    @Query("UPDATE decks SET isDeleted = 1, synced = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE flashcards SET isDeleted = 1, synced = 0, updatedAt = :now WHERE deckId = :deckId AND isDeleted = 0")
    suspend fun softDeleteCardsOf(deckId: String, now: Long)

    @Query("SELECT imagePath FROM notes WHERE deckId = :deckId AND isDeleted = 0")
    suspend fun noteImagePathsOf(deckId: String): List<String>

    @Query("UPDATE notes SET isDeleted = 1, synced = 0, updatedAt = :now WHERE deckId = :deckId AND isDeleted = 0")
    suspend fun softDeleteNotesOf(deckId: String, now: Long)

    /**
     * Xoá bộ thẻ KÈM các thẻ và ghi chú (ảnh chụp + văn bản) bên trong. Nếu chỉ đánh dấu xoá bộ
     * thẻ thì thẻ của nó thành "mồ côi": không còn thấy ở đâu nhưng vẫn bị đếm/lấy ra khi ôn tập.
     * `@Transaction`: các lệnh UPDATE hoặc cùng thành công, hoặc cùng không có gì thay đổi.
     *
     * Trả về đường dẫn ảnh của các ghi chú vừa xoá. Database chỉ giữ ĐƯỜNG DẪN, còn file ảnh nằm
     * ngoài database nên nơi gọi phải tự xoá file — và chỉ xoá SAU khi transaction đã xong.
     */
    @Transaction
    suspend fun softDeleteWithContents(id: String, now: Long): List<String> {
        val imagePaths = noteImagePathsOf(id) // phải đọc trước khi ghi chú bị đánh dấu xoá
        softDelete(id, now)
        softDeleteCardsOf(id, now)
        softDeleteNotesOf(id, now)
        return imagePaths
    }
}
