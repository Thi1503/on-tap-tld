package com.ledinhthi.ontaptld.feature.capture.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import com.ledinhthi.ontaptld.core.domain.util.noteLines
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.feature.capture.domain.SourceLineLocator
import com.ledinhthi.ontaptld.feature.capture.domain.model.OcrLine
import com.ledinhthi.ontaptld.feature.capture.domain.repository.CaptureImageRepository
import com.ledinhthi.ontaptld.feature.capture.domain.repository.OcrRepository
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
 *
 * Mỗi thẻ AI được lưu kèm số dòng ghi chú mà nó được rút ra và, nếu tìm được, vị trí của dòng
 * đó trên ảnh — xem [SourceLineLocator].
 */
class SaveSuggestedCardsUseCase @Inject constructor(
    private val imageRepository: CaptureImageRepository,
    private val ocrRepository: OcrRepository,
    private val sourceLineLocator: SourceLineLocator,
    private val noteRepository: NoteRepository,
    private val createFlashcardsFromNote: CreateFlashcardsFromNoteUseCase,
    private val clock: Clock,
    private val ids: IdGenerator,
) : UseCase<SaveSuggestedCardsUseCase.Params, List<Flashcard>>() {

    /**
     * [fromAi] = false cho thẻ người dùng tự gõ thêm trong màn duyệt. [sourceLine] = dòng ghi
     * chú mà AI rút thẻ ra (đếm từ 1); null nếu không rõ.
     */
    data class CardDraft(
        val question: String,
        val answer: String,
        val fromAi: Boolean,
        val sourceLine: Int? = null,
    )

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
            val noteLines = noteLines(input.noteText)
            val ocrLines = recognizeLinesOrEmpty(input)
            return createFlashcardsFromNote(
                CreateFlashcardsFromNoteUseCase.Params(
                    deckId = input.deckId,
                    noteId = noteId,
                    drafts = input.cards.map { card ->
                        // Chỉ giữ số dòng nếu nó thật sự trỏ vào một dòng của ghi chú.
                        val line = card.sourceLine?.takeIf { card.fromAi && it in 1..noteLines.size }
                        CreateFlashcardsFromNoteUseCase.Draft(
                            question = card.question,
                            answer = card.answer,
                            // Số dòng đếm từ 1, còn vị trí trong danh sách đếm từ 0 nên phải trừ 1.
                            box = line?.let { sourceLineLocator.locate(noteLines[it - 1], it - 1, ocrLines) },
                            sourceLine = line,
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

    /**
     * Đọc lại vị trí từng dòng chữ trên ảnh, để biết mỗi thẻ AI nằm ở đâu trên ảnh (vùng được
     * tô sáng khi xem ảnh nguồn). Chạy lại bộ nhận dạng ở đây thay vì mang kết quả từ bước 2
     * sang: nhận dạng chạy ngay trên máy, mất chừng một giây, và nhờ vậy không phải giữ thêm dữ
     * liệu qua hai màn (kể cả khi app bị hệ thống tắt giữa chừng).
     *
     * Chỉ là phần "có thì tốt": không có thẻ AI nào cần tìm thì khỏi đọc, và đọc lỗi thì vẫn
     * lưu thẻ bình thường — thẻ chỉ thiếu vùng tô sáng.
     */
    private suspend fun recognizeLinesOrEmpty(input: Params): List<OcrLine> {
        if (input.cards.none { it.fromAi && it.sourceLine != null }) return emptyList()
        return try {
            ocrRepository.recognizeLines(input.imagePath)
        } catch (e: AppException.OcrException) {
            emptyList()
        }
    }
}
