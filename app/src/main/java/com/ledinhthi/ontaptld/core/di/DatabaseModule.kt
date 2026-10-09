package com.ledinhthi.ontaptld.core.di

import android.content.Context
import androidx.room.Room
import com.ledinhthi.ontaptld.core.data.local.db.ALL_MIGRATIONS
import com.ledinhthi.ontaptld.core.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "ontaptld.db")
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides
    fun provideSyncQueueDao(db: AppDatabase) = db.syncQueueDao()

    @Provides
    fun provideSyncDao(db: AppDatabase) = db.syncDao()

    @Provides
    fun provideReviewLogDao(db: AppDatabase) = db.reviewLogDao()

    @Provides
    fun provideDeckDao(db: AppDatabase) = db.deckDao()

    @Provides
    fun provideNoteDao(db: AppDatabase) = db.noteDao()

    @Provides
    fun provideFlashcardDao(db: AppDatabase) = db.flashcardDao()
}
