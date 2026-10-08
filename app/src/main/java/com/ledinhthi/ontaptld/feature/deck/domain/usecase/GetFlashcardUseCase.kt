package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import javax.inject.Inject

/** Đọc MỘT thẻ theo id (đọc một lần, không nghe thay đổi) — dùng để nạp nội dung vào màn sửa thẻ. */
class GetFlashcardUseCase @Inject constructor(
    private val repository: FlashcardRepository,
) : UseCase<String, Flashcard?>() {
    override suspend fun invoke(input: String): Flashcard? = repository.getById(input)
}
