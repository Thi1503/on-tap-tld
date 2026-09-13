package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import javax.inject.Inject

class DeleteDeckUseCase @Inject constructor(
    private val repository: DeckRepository,
) : UseCase<String, Unit>() {
    override suspend fun invoke(input: String) = repository.delete(input)
}
