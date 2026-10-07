package com.ledinhthi.ontaptld.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data object LoginRoute // feature/auth — Sprint 2 (auth/sync), UI dựng trước ở Sprint 1

@Serializable
data object HomeRoute

@Serializable
data class DeckDetailRoute(val deckId: String)

@Serializable
data class ManualCardRoute(val deckId: String)

@Serializable
data object CaptureRoute // feature/capture — Sprint 1 tuần 2

@Serializable
data class ReviewRoute(val deckId: String? = null) // feature/review — Sprint 1 tuần 3

@Serializable
data object SettingsRoute
