package com.ledinhthi.ontaptld.feature.deck.domain.model

import com.ledinhthi.ontaptld.core.domain.model.DomainModel

/** Số liệu thẻ gom theo 1 bộ thẻ — kết quả của 1 câu GROUP BY, chưa gắn với [Deck]. */
data class DeckCardStats(
    val deckId: String,
    val cardCount: Int,
    val dueCount: Int,
) : DomainModel

/** 1 dòng trên Home: bộ thẻ + tổng số thẻ + số thẻ cần ôn hôm nay. */
data class DeckSummary(
    val deck: Deck,
    val cardCount: Int = 0,
    val dueCount: Int = 0,
) : DomainModel
