package com.ledinhthi.ontaptld.feature.review.domain.model

import com.ledinhthi.ontaptld.core.domain.model.DomainModel
import com.ledinhthi.ontaptld.core.domain.util.noteLines
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.Note
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade

/**
 * Một thẻ trong phiên ôn, đi kèm bộ thẻ của nó (màn ôn cần tên và màu bộ thẻ).
 *
 * @param source ghi chú mà thẻ AI được rút ra; null với thẻ thủ công, hoặc khi ghi chú không còn.
 */
data class ReviewCard(
    val card: Flashcard,
    val deck: Deck,
    val source: CardSource? = null,
) : DomainModel

/**
 * Nguồn gốc của một thẻ AI, để mặt đáp án cho xem lại thẻ được rút ra từ đâu.
 *
 * @param capturedAt lúc ghi chú được lưu (mốc thời gian dạng mili giây).
 * @param excerpt đoạn trích ghi chú: dòng nguồn của thẻ cùng dòng ngay trước và ngay sau. Rỗng
 * khi không biết thẻ lấy từ dòng nào.
 */
data class CardSource(
    val capturedAt: Long,
    val excerpt: List<ExcerptLine>,
) : DomainModel

/** Một dòng trong đoạn trích; [isSource] = true cho đúng dòng mà thẻ được rút ra. */
data class ExcerptLine(val text: String, val isSource: Boolean)

/**
 * Dựng [CardSource] cho một thẻ từ ghi chú của nó. [sourceLine] là số dòng lưu cùng thẻ (đếm từ
 * 1, theo cách đánh số của `noteLines`).
 */
fun Note.toCardSource(sourceLine: Int?): CardSource {
    val lines = noteLines(ocrText)
    val index = sourceLine?.minus(1) // vị trí trong danh sách đếm từ 0
    val excerpt = if (index == null || index !in lines.indices) {
        emptyList()
    } else {
        // Lấy dòng trước, dòng nguồn, dòng sau; `coerceAtLeast` / `coerceAtMost` giữ khoảng này
        // không tràn khỏi hai đầu danh sách khi dòng nguồn là dòng đầu hoặc dòng cuối ghi chú.
        val range = (index - 1).coerceAtLeast(0)..(index + 1).coerceAtMost(lines.lastIndex)
        range.map { i -> ExcerptLine(text = lines[i], isSource = i == index) }
    }
    return CardSource(capturedAt = createdAt, excerpt = excerpt)
}

/** Một dòng lịch sử: thẻ nào được chấm mức nào, lúc nào. Nguồn số liệu cho màn Thống kê (bước 7). */
data class ReviewLog(
    val id: String,
    val cardId: String,
    val deckId: String,
    val grade: ReviewGrade,
    val reviewedAt: Long,
) : DomainModel
