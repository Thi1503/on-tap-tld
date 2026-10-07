package com.ledinhthi.ontaptld.feature.auth.presentation.login

import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.SnackBarType
import com.ledinhthi.ontaptld.navigation.HomeRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Sprint 1: chưa có `LoginUseCase`/backend auth thật (xem docs Sprint 2 — auth/sync).
 * ViewModel chỉ validate form phía UI; khi hợp lệ, coi như đăng nhập demo và vào thẳng
 * Home (giữ đúng tinh thần "Sprint 1 không chặn auth thật" trước khi Login được thêm vào).
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
) : BaseViewModel<LoginState>(LoginState(), toolbox) {

    fun onEmailChange(value: String) = setState { copy(email = value, emailError = null) }
    fun onPasswordChange(value: String) = setState { copy(password = value, passwordError = null) }
    fun onTogglePasswordVisibility() = setState { copy(isPasswordVisible = !isPasswordVisible) }

    fun onLoginClick() {
        val emailError = validateEmail(currentState.email)
        val passwordError = validatePassword(currentState.password)
        if (emailError != null || passwordError != null) {
            setState { copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        launchGuarded(showLoadingOverlay = true) {
            delay(400)
            navigator.showSnackBar("Đăng nhập demo — Sprint 2 sẽ kết nối backend thật.", SnackBarType.INFO)
            navigator.replaceAll(HomeRoute)
        }
    }

    fun onForgotPasswordClick() =
        navigator.showSnackBar("Tính năng quên mật khẩu đang được phát triển.", SnackBarType.INFO)

    fun onSignUpClick() =
        navigator.showSnackBar("Tính năng đăng ký đang được phát triển.", SnackBarType.INFO)

    private fun validateEmail(email: String): String? = when {
        email.isBlank() -> "Vui lòng nhập email"
        !email.contains("@") || !email.substringAfter("@").contains(".") -> "Email không hợp lệ"
        else -> null
    }

    private fun validatePassword(password: String): String? = when {
        password.isBlank() -> "Vui lòng nhập mật khẩu"
        password.length < 6 -> "Mật khẩu tối thiểu 6 ký tự"
        else -> null
    }
}
