package com.ledinhthi.ontaptld.feature.auth.presentation.signin

import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus

/** Màn này không có dữ liệu riêng; chỉ cần [status] để hiện lớp "đang xử lý" lúc đăng nhập. */
data class GoogleSignInState(
    override val status: UiStatus = UiStatus(),
) : UiState {
    override fun withStatus(status: UiStatus) = copy(status = status)
}
