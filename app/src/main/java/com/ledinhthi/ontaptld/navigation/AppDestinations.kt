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
data object CaptureRoute // feature/capture — Sprint 1 tuần 2

@Serializable
data class ReviewRoute(val deckId: String? = null) // feature/review — Sprint 1 tuần 3

@Serializable
data object SettingsRoute
