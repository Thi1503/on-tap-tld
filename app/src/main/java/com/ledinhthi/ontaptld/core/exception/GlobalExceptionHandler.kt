package com.ledinhthi.ontaptld.core.exception

import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.navigation.SnackBarType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nơi DUY NHẤT quyết định "lỗi này thì hiện dialog hay snackbar".
 * Vai trò giống hệt `ExceptionHandler` của base Flutter, đổi nhánh từ HTTP code sang loại lỗi app.
 *
 * i18n: literal tiếng Việt chỉ để chạy Sprint 1. Sprint 2 truyền `StringProvider` qua constructor.
 */
@Singleton
class GlobalExceptionHandler @Inject constructor(
    private val navigator: AppNavigator,
) {
    fun handle(wrapper: AppExceptionWrapper) {
        when (val e = wrapper.exception) {
            is AppException.AiException -> handleAi(e, wrapper.overrideMessage)
            is AppException.OcrException -> navigator.showSnackBar(
                "Không nhận được chữ từ ảnh, thử chụp rõ hơn.", SnackBarType.FAILURE,
            )
            is AppException.LocalException -> navigator.showErrorDialog(
                "Không đọc/ghi được dữ liệu trên máy. Thử khởi động lại app.",
            )
            is AppException.CustomException -> navigator.showSnackBar(
                wrapper.overrideMessage ?: e.userMessage ?: "Đã có lỗi xảy ra.",
            )
            is AppException.UncaughtException -> navigator.showSnackBar("Đã có lỗi xảy ra.")
        }
    }

    private fun handleAi(e: AppException.AiException, override: String?) = when (e.kind) {
        AiErrorKind.QUOTA_EXCEEDED_LOCAL, AiErrorKind.QUOTA_EXCEEDED_SERVER ->
            navigator.showNotificationDialog("Bạn đã dùng hết lượt tạo thẻ bằng AI hôm nay. Thử lại vào ngày mai nhé.")
        AiErrorKind.NETWORK ->
            navigator.showErrorDialog("Cần mạng để tạo thẻ bằng AI. Tạo thẻ thủ công vẫn dùng offline bình thường.")
        AiErrorKind.CONTENT_BLOCKED ->
            navigator.showNotificationDialog("Nội dung này không tạo được thẻ bằng AI. Bạn có thể tự thêm thẻ thủ công.")
        AiErrorKind.RESPONSE_PARSE_ERROR, AiErrorKind.EMPTY_RESPONSE ->
            navigator.showSnackBar("AI trả về kết quả không hợp lệ, thử lại giúp mình.")
        AiErrorKind.APP_CHECK_FAILED ->
            navigator.showErrorDialog("Lỗi xác thực ứng dụng. Cập nhật app lên bản mới nhất.")
        AiErrorKind.NOT_CONFIGURED ->
            navigator.showErrorDialog("Tính năng AI chưa được bật trong bản build này.")
        AiErrorKind.MODEL_UNAVAILABLE, AiErrorKind.UNKNOWN ->
            navigator.showSnackBar(override ?: "Dịch vụ AI tạm thời không dùng được.")
    }
}
