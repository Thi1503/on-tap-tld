package com.ledinhthi.ontaptld.feature.auth.presentation.signin

import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.core.presentation.navigation.SnackBarType
import com.ledinhthi.ontaptld.feature.auth.domain.usecase.SignInWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel của màn Đăng nhập Google (route `GoogleSignInRoute`), mở từ màn Cài đặt.
 *
 * Việc đăng nhập có hai chặng: (1) màn hình mở hộp chọn tài khoản của hệ thống và nhận về một ID
 * token — xem `requestGoogleIdToken`; (2) ViewModel này mang token đó đi đăng nhập Firebase.
 */
@HiltViewModel
class GoogleSignInViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    private val signInWithGoogle: SignInWithGoogleUseCase,
) : BaseViewModel<GoogleSignInState>(GoogleSignInState(), toolbox) {

    /** Nút ←, Back của máy và nút "Để sau" đều chỉ lùi về màn Cài đặt. */
    fun onBack() = navigator.back()

    /** Màn hình gọi hàm này khi hộp chọn tài khoản đóng lại, kèm kết quả. */
    fun onGoogleIdTokenResult(result: GoogleIdTokenResult) {
        if (result is GoogleIdTokenResult.Cancelled) return // người dùng tự đóng hộp chọn: im lặng
        if (currentState.status.isLoadingOverlay) return // đang đăng nhập dở, bỏ qua lần gọi thừa

        // Lỗi ném ra trong khối này được `launchGuarded` bắt và chuyển cho bộ xử lý lỗi chung,
        // nơi quyết định câu báo cho từng loại `AuthErrorKind`.
        launchGuarded(showLoadingOverlay = true) {
            val idToken = when (result) {
                is GoogleIdTokenResult.Success -> result.idToken
                is GoogleIdTokenResult.Failure -> throw AppException.AuthException(result.kind)
                GoogleIdTokenResult.Cancelled -> return@launchGuarded
            }
            val user = signInWithGoogle(idToken)
            // Ưu tiên email (thứ phân biệt được hai tài khoản), không có thì dùng tên.
            val who = user.email ?: user.displayName
            val message = if (who != null) {
                strings.get(R.string.auth_signed_in_as, who)
            } else {
                strings.get(R.string.auth_signed_in)
            }
            navigator.showSnackBar(message, SnackBarType.SUCCESS)
            navigator.back()
        }
    }
}
