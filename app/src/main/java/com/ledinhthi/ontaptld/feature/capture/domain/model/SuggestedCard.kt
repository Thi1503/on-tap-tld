package com.ledinhthi.ontaptld.feature.capture.domain.model

/**
 * Một thẻ do AI đề xuất, CHƯA lưu vào bộ thẻ nào — người dùng còn duyệt, sửa hoặc bỏ.
 *
 * [sourceLine]: thẻ được rút ra từ dòng thứ mấy của ghi chú (tính từ 1, chỉ đếm dòng có chữ);
 * null khi AI không chỉ ra được.
 */
data class SuggestedCard(
    val question: String,
    val answer: String,
    val sourceLine: Int? = null,
)
