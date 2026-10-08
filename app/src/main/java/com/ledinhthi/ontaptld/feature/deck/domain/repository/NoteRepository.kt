package com.ledinhthi.ontaptld.feature.deck.domain.repository

import com.ledinhthi.ontaptld.feature.deck.domain.model.Note

interface NoteRepository {
    suspend fun getById(id: String): Note?
    suspend fun upsert(note: Note)
    suspend fun delete(id: String)

    /**
     * Dọn những gì còn sót: ghi chú không còn thẻ nào dùng, và file ảnh không còn ghi chú nào
     * trỏ tới (xoá thẻ từ bản app cũ, hoặc app bị tắt giữa lúc đang xoá / đang lưu).
     */
    suspend fun cleanUpOrphans()
}
