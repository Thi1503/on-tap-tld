package com.ledinhthi.ontaptld.feature.deck.domain.repository

import com.ledinhthi.ontaptld.feature.deck.domain.model.Note

interface NoteRepository {
    suspend fun getById(id: String): Note?
    suspend fun upsert(note: Note)
    suspend fun delete(id: String)
}
