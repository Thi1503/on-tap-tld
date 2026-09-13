package com.ledinhthi.ontaptld.core.di

import android.content.Context
import androidx.room.Room
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
            // .addMigrations(MIGRATION_1_2, …)   // BẮT BUỘC sau bản Closed Testing đầu tiên (docs Mục 7.3)
            .build()

    @Provides
    fun provideDeckDao(db: AppDatabase) = db.deckDao()

    @Provides
    fun provideNoteDao(db: AppDatabase) = db.noteDao()

    @Provides
    fun provideFlashcardDao(db: AppDatabase) = db.flashcardDao()
}
