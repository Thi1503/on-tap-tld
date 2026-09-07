package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.FlowUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFlashcardsUseCase @Inject constructor(
    private val repository: FlashcardRepository,
) : FlowUseCase<String, List<Flashcard>>() {
    override fun invoke(input: String): Flow<List<Flashcard>> = repository.observeByDeck(input)
}
