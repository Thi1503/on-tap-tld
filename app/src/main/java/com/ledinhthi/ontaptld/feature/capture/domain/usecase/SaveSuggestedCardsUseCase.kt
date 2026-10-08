package com.ledinhthi.ontaptld.feature.capture.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import com.ledinhthi.ontaptld.feature.capture.domain.repository.CaptureImageRepository
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.deck.domain.model.Note
import com.ledinhthi.ontaptld.feature.deck.domain.repository.NoteRepository
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CreateFlashcardsFromNoteUseCase
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Bước cuối của luồng tạo thẻ bằng AI: lưu những thẻ người dùng đã duyệt vào bộ thẻ, kèm "ghi
 * chú" gốc (ảnh đã cắt + văn bản) để sau này xem lại thẻ được rút ra từ đâu.
 *
 * Thứ tự: giữ ảnh → ghi ghi chú → ghi thẻ. Hỏng ở bước nào thì dọn những gì đã làm trước đó,
 * để không còn sót ảnh hay ghi chú "mồ côi" không thẻ nào trỏ tới.
 */
class SaveSuggestedCardsUseCase @Inject constructor(
    private val imageRepository: CaptureImageRepository,
    private val noteRepository: NoteRepository,
    private val createFlashcardsFromNote: CreateFlashcardsFromNoteUseCase,
    private val clock: Clock,
    private val ids: IdGenerator,
) : UseCase<SaveSuggestedCardsUseCase.Params, List<Flashcard>>() {

    /** [fromAi] = false cho thẻ người dùng tự gõ thêm trong màn duyệt. */
    data class CardDraft(val question: String, val answer: String, val fromAi: Boolean)

    data class Params(
        val deckId: String,
        /** Ảnh đã cắt, đang nằm ở thư mục tạm. */
        val imagePath: String,
        val noteText: String,
        val cards: List<CardDraft>,
    )

    override suspend fun invoke(input: Params): List<Flashcard> {
        val noteId = ids.newId()
        val keptImagePath = imageRepository.keepNoteImage(input.imagePath, noteId)
        try {
            val now = clock.nowMillis()
            noteRepository.upsert(
                Note(
                    id = noteId,
                    deckId = input.deckId,
                    imagePath = keptImagePath,
                    ocrText = input.noteText,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            return createFlashcardsFromNote(
                CreateFlashcardsFromNoteUseCase.Params(
                    deckId = input.deckId,
                    noteId = noteId,
                    drafts = input.cards.map { card ->
                        CreateFlashcardsFromNoteUseCase.Draft(
                            question = card.question,
                            answer = card.answer,
                            box = null, // vùng nguồn trên ảnh sẽ có khi làm phần "xem ảnh nguồn"
                            source = if (card.fromAi) FlashcardSource.AI else FlashcardSource.MANUAL,
                        )
                    },
                ),
            )
        } catch (e: CancellationException) {
            throw e // bị huỷ giữa chừng không phải lỗi; đừng "nuốt" tín hiệu huỷ của coroutine
        } catch (e: Exception) {
            // `runCatching` bọc việc dọn dẹp: nếu chính nó cũng lỗi thì bỏ qua, để lỗi GỐC (lý do
            // thật khiến lưu hỏng) vẫn là lỗi được báo ra ngoài.
            runCatching { noteRepository.delete(noteId) }
            runCatching { imageRepository.deleteNoteImage(keptImagePath) }
            throw e
        }
    }
}
