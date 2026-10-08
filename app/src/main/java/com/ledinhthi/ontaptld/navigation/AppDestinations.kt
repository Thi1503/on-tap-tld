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

/** [cardId] = null: thêm thẻ mới vào bộ [deckId]; có giá trị: sửa thẻ đó. */
@Serializable
data class ManualCardRoute(val deckId: String, val cardId: String? = null)

@Serializable
data object CaptureRoute // feature/capture — bước 4

/** [deckId] = null: ôn mọi thẻ đến hạn; có giá trị: chỉ ôn thẻ của bộ đó. */
@Serializable
data class ReviewRoute(val deckId: String? = null)

@Serializable
data object SettingsRoute
