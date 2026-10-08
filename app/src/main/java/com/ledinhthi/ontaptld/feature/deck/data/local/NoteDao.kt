package com.ledinhthi.ontaptld.feature.deck.data.local

import androidx.room.Dao
import androidx.room.Query
import com.ledinhthi.ontaptld.core.data.local.db.BaseDao

@Dao
interface NoteDao : BaseDao<NoteEntity> {
    @Query("SELECT * FROM notes WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): NoteEntity?

    @Query("UPDATE notes SET isDeleted = 1, synced = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT imagePath FROM notes WHERE isDeleted = 0")
    suspend fun liveImagePaths(): List<String>

    /**
     * Đánh dấu xoá các ghi chú "mồ côi": không còn thẻ nào (chưa xoá) trỏ tới. Trả về số ghi chú
     * vừa xoá.
     *
     * `NOT EXISTS (SELECT 1 …)`: với mỗi ghi chú, thử tìm MỘT thẻ còn sống của nó; không tìm ra
     * thì ghi chú đó mồ côi. [createdBefore] chừa lại ghi chú vừa tạo — lúc lưu thẻ AI, ghi chú
     * được ghi trước rồi mới tới thẻ, nên trong khoảnh khắc đó nó chưa có thẻ nào.
     */
    @Query(
        "UPDATE notes SET isDeleted = 1, synced = 0, updatedAt = :now " +
            "WHERE isDeleted = 0 AND createdAt < :createdBefore AND NOT EXISTS (" +
            "SELECT 1 FROM flashcards WHERE flashcards.noteId = notes.id AND flashcards.isDeleted = 0)",
    )
    suspend fun softDeleteOrphans(now: Long, createdBefore: Long): Int
}
