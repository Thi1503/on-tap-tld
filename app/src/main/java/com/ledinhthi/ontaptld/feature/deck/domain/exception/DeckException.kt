package com.ledinhthi.ontaptld.feature.deck.domain.exception

import com.ledinhthi.ontaptld.core.exception.AppException

class DeckException(val kind: Kind) : AppException.CustomException(
    userMessage = when (kind) {
        Kind.BLANK_NAME -> "Tên deck không được để trống."
        Kind.BLANK_CARD -> "Câu hỏi và câu trả lời không được để trống."
        Kind.DECK_NOT_FOUND -> "Không tìm thấy deck."
        Kind.CARD_NOT_FOUND -> "Không tìm thấy thẻ."
    },
) {
    enum class Kind { BLANK_NAME, BLANK_CARD, DECK_NOT_FOUND, CARD_NOT_FOUND }
}
