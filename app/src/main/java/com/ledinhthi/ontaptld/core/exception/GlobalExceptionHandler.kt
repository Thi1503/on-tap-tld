package com.ledinhthi.ontaptld.core.exception

import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.navigation.SnackBarType
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nơi DUY NHẤT quyết định "lỗi này thì hiện dialog hay snackbar".
 * Vai trò giống hệt `ExceptionHandler` của base Flutter, đổi nhánh từ HTTP code sang loại lỗi app.
 */
@Singleton
class GlobalExceptionHandler @Inject constructor(
    private val navigator: AppNavigator,
    private val strings: StringProvider,
) {
    fun handle(wrapper: AppExceptionWrapper) {
        when (val e = wrapper.exception) {
            is AppException.AiException -> handleAi(e, wrapper.overrideMessage)
            is AppException.OcrException -> navigator.showSnackBar(
                strings.get(R.string.error_ocr_no_text), SnackBarType.FAILURE,
            )
            is AppException.AuthException -> navigator.showSnackBar(
                strings.get(
                    when (e.kind) {
                        AuthErrorKind.NOT_CONFIGURED -> R.string.auth_error_not_configured
                        AuthErrorKind.NETWORK -> R.string.auth_error_network
                        AuthErrorKind.NO_GOOGLE_ACCOUNT -> R.string.auth_error_no_account
                        AuthErrorKind.UNKNOWN -> R.string.auth_error_unknown
                    },
                ),
                SnackBarType.FAILURE,
            )
            is AppException.SyncException -> navigator.showSnackBar(
                strings.get(
                    when (e.kind) {
                        SyncErrorKind.NOT_SIGNED_IN -> R.string.sync_error_not_signed_in
                        SyncErrorKind.NOT_CONFIGURED -> R.string.sync_error_not_configured
                        SyncErrorKind.NETWORK -> R.string.sync_error_network
                        SyncErrorKind.PERMISSION_DENIED -> R.string.sync_error_permission
                        SyncErrorKind.UNKNOWN -> R.string.sync_error_unknown
                    },
                ),
                SnackBarType.FAILURE,
            )
            is AppException.LocalException -> navigator.showErrorDialog(
                strings.get(R.string.error_local_storage),
            )
            is AppException.CustomException -> navigator.showSnackBar(
                wrapper.overrideMessage ?: e.userMessage ?: strings.get(R.string.error_generic),
            )
            is AppException.UncaughtException -> navigator.showSnackBar(strings.get(R.string.error_generic))
        }
    }

    private fun handleAi(e: AppException.AiException, override: String?) = when (e.kind) {
        AiErrorKind.QUOTA_EXCEEDED_LOCAL, AiErrorKind.QUOTA_EXCEEDED_SERVER ->
            navigator.showNotificationDialog(strings.get(R.string.error_ai_quota))
        AiErrorKind.NETWORK ->
            navigator.showErrorDialog(strings.get(R.string.error_ai_network))
        AiErrorKind.CONTENT_BLOCKED ->
            navigator.showNotificationDialog(strings.get(R.string.error_ai_content_blocked))
        AiErrorKind.RESPONSE_PARSE_ERROR ->
            navigator.showSnackBar(strings.get(R.string.error_ai_bad_response))
        AiErrorKind.EMPTY_RESPONSE ->
            navigator.showNotificationDialog(strings.get(R.string.error_ai_no_cards))
        AiErrorKind.APP_CHECK_FAILED ->
            navigator.showErrorDialog(strings.get(R.string.error_ai_app_check))
        AiErrorKind.NOT_CONFIGURED ->
            navigator.showErrorDialog(strings.get(R.string.error_ai_not_configured))
        AiErrorKind.MODEL_UNAVAILABLE, AiErrorKind.UNKNOWN ->
            navigator.showSnackBar(override ?: strings.get(R.string.error_ai_unavailable))
    }
}
