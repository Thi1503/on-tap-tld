package com.ledinhthi.ontaptld.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data object LoginRoute // feature/auth — chưa đăng ký trong AppNavHost; dành cho đăng nhập Google (bước 6)

@Serializable
data object HomeRoute

@Serializable
data class DeckDetailRoute(val deckId: String)

/**
 * [cardId] = null: thêm thẻ mới vào bộ [deckId]; có giá trị: sửa thẻ đó.
 * [noteText]: văn bản ghi chú hiện kèm để vừa nhìn vừa gõ thẻ — chỉ truyền khi mở từ màn AI lỗi /
 * hết lượt (nút "Tự gõ thẻ từ văn bản này").
 */
@Serializable
data class ManualCardRoute(val deckId: String, val cardId: String? = null, val noteText: String? = null)

/**
 * Bước 1/3 của luồng "chụp ghi chú → AI tạo thẻ". [deckId] = bộ thẻ sẽ được chọn sẵn ở bước 2
 * (mở từ Chi tiết bộ thẻ); null khi mở từ Home.
 */
@Serializable
data class CaptureRoute(val deckId: String? = null)

/** Bước 2/3: [imagePath] là file ảnh ĐÃ CẮT mà màn chụp vừa tạo ra. */
@Serializable
data class OcrReviewRoute(val imagePath: String, val deckId: String? = null)

/**
 * Bước 3/3: gửi [noteText] (văn bản người dùng đã kiểm tra) cho AI, rồi duyệt thẻ đề xuất và lưu
 * vào bộ [deckId]. [imagePath] là ảnh nguồn sẽ được giữ lại cùng các thẻ.
 */
@Serializable
data class AiCardsRoute(val imagePath: String, val noteText: String, val deckId: String)

/** [deckId] = null: ôn mọi thẻ đến hạn; có giá trị: chỉ ôn thẻ của bộ đó. */
@Serializable
data class ReviewRoute(val deckId: String? = null)

/** Xem cả ảnh ghi chú mà thẻ AI [cardId] được rút ra, có tô sáng vùng của thẻ. */
@Serializable
data class SourceImageRoute(val cardId: String)

@Serializable
data object SettingsRoute
