package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import javax.inject.Inject

class DeleteFlashcardUseCase @Inject constructor(
    private val repository: FlashcardRepository,
) : UseCase<String, Unit>() {
    override suspend fun invoke(input: String) = repository.delete(input)
}
