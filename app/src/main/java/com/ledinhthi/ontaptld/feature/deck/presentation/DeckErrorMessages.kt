package com.ledinhthi.ontaptld.feature.deck.presentation

import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException

/** Thông báo hiển thị (đã dịch) cho lỗi nghiệp vụ của feature deck — domain chỉ mang `kind`. */
fun DeckException.displayMessage(strings: StringProvider): String = strings.get(
    when (kind) {
        DeckException.Kind.BLANK_NAME -> R.string.deck_error_blank_name
        DeckException.Kind.BLANK_CARD -> R.string.deck_error_blank_card
        DeckException.Kind.DECK_NOT_FOUND -> R.string.deck_error_deck_not_found
        DeckException.Kind.CARD_NOT_FOUND -> R.string.deck_error_card_not_found
    },
)
