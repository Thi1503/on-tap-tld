package com.ledinhthi.ontaptld.feature.deck.di

import com.ledinhthi.ontaptld.feature.deck.data.repository.DeckRepositoryImpl
import com.ledinhthi.ontaptld.feature.deck.data.repository.FlashcardRepositoryImpl
import com.ledinhthi.ontaptld.feature.deck.data.repository.NoteRepositoryImpl
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import com.ledinhthi.ontaptld.feature.deck.domain.repository.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DeckModule {
    @Binds
    @Singleton
    fun deckRepository(impl: DeckRepositoryImpl): DeckRepository

    @Binds
    @Singleton
    fun flashcardRepository(impl: FlashcardRepositoryImpl): FlashcardRepository

    @Binds
    @Singleton
    fun noteRepository(impl: NoteRepositoryImpl): NoteRepository
}
