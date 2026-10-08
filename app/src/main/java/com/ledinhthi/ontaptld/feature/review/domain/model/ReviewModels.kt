package com.ledinhthi.ontaptld.feature.review.domain.model

import com.ledinhthi.ontaptld.core.domain.model.DomainModel
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade

/** Một thẻ trong phiên ôn, đi kèm bộ thẻ của nó (màn ôn cần tên và màu bộ thẻ). */
data class ReviewCard(
    val card: Flashcard,
    val deck: Deck,
) : DomainModel

/** Một dòng lịch sử: thẻ nào được chấm mức nào, lúc nào. Nguồn số liệu cho màn Thống kê (bước 7). */
data class ReviewLog(
    val id: String,
    val cardId: String,
    val deckId: String,
    val grade: ReviewGrade,
    val reviewedAt: Long,
) : DomainModel
