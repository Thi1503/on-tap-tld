package com.ledinhthi.ontaptld.feature.deck.data.repository

import com.ledinhthi.ontaptld.core.data.local.db.wrapLocal
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteDao
import com.ledinhthi.ontaptld.feature.deck.data.mapper.NoteEntityMapper
import com.ledinhthi.ontaptld.feature.deck.domain.model.Note
import com.ledinhthi.ontaptld.feature.deck.domain.repository.NoteRepository
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val dao: NoteDao,
    private val mapper: NoteEntityMapper,
    private val clock: Clock,
) : NoteRepository {

    override suspend fun getById(id: String): Note? =
        wrapLocal { dao.getById(id)?.let(mapper::toDomain) }

    override suspend fun upsert(note: Note) = wrapLocal { dao.upsert(mapper.toEntity(note)) }

    override suspend fun delete(id: String) = wrapLocal { dao.softDelete(id, clock.nowMillis()) }
}
