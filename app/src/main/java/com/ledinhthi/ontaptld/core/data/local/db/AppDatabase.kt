package com.ledinhthi.ontaptld.core.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ledinhthi.ontaptld.core.sync.SyncQueueDao
import com.ledinhthi.ontaptld.core.sync.SyncQueueEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckDao
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardDao
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteDao
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteEntity
import com.ledinhthi.ontaptld.feature.review.data.local.ReviewLogDao
import com.ledinhthi.ontaptld.feature.review.data.local.ReviewLogEntity

@Database(
    entities = [
        DeckEntity::class,
        NoteEntity::class,
        FlashcardEntity::class,
        SyncQueueEntity::class,
        ReviewLogEntity::class,
    ],
    version = 2, // đổi schema -> tăng version + thêm Migration vào Migrations.kt (docs Mục 7.3)
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun noteDao(): NoteDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun reviewLogDao(): ReviewLogDao
}
