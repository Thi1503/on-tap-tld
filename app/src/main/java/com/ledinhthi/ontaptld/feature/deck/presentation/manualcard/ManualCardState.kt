package com.ledinhthi.ontaptld.feature.deck.presentation.manualcard

import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus

data class ManualCardState(
    override val status: UiStatus = UiStatus(),
    val question: String = "",
    val answer: String = "",
) : UiState {
    override fun withStatus(status: UiStatus) = copy(status = status)
}
