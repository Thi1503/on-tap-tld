package com.ledinhthi.ontaptld.core.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckDao
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardDao
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteDao
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteEntity

@Database(
    entities = [
        DeckEntity::class,
        NoteEntity::class,
        FlashcardEntity::class,
        // SyncQueueEntity::class,   // Sprint 2
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun noteDao(): NoteDao
    abstract fun flashcardDao(): FlashcardDao
}
