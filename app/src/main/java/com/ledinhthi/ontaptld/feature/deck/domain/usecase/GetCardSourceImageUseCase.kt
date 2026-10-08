package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.feature.deck.domain.model.SourceBox
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import com.ledinhthi.ontaptld.feature.deck.domain.repository.NoteRepository
import javax.inject.Inject

/**
 * Ảnh ghi chú mà một thẻ AI được rút ra: đường dẫn file ảnh và vùng của thẻ trên ảnh.
 * [box] = null khi không xác định được thẻ nằm ở đâu trên ảnh — vẫn xem được cả ảnh.
 */
data class CardSourceImage(val imagePath: String, val box: SourceBox?)

/**
 * Tìm ảnh nguồn của thẻ có id cho trước. Trả về null nếu thẻ không còn, thẻ không gắn với ghi
 * chú nào (thẻ thủ công), hoặc ghi chú đã bị xoá.
 */
class GetCardSourceImageUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val noteRepository: NoteRepository,
) : UseCase<String, CardSourceImage?>() {

    override suspend fun invoke(input: String): CardSourceImage? {
        val card = flashcardRepository.getById(input) ?: return null
        val note = card.noteId?.let { noteRepository.getById(it) } ?: return null
        return CardSourceImage(imagePath = note.imagePath, box = card.sourceBox)
    }
}
