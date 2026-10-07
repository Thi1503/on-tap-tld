package com.ledinhthi.ontaptld.feature.auth.presentation.login

import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus

data class LoginState(
    override val status: UiStatus = UiStatus(),
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
) : UiState {
    override fun withStatus(status: UiStatus) = copy(status = status)
}
