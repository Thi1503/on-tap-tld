package com.ledinhthi.ontaptld.core.exception

sealed class AppException(open val userMessage: String? = null) : Exception() {

    /** Lỗi Room / DataStore / IO local. */
    data class LocalException(
        val kind: LocalErrorKind,
        override val cause: Throwable? = null,
    ) : AppException()

    /** Lỗi khi gọi Gemini qua Firebase AI Logic. */
    data class AiException(
        val kind: AiErrorKind,
        val httpStatus: Int? = null,
        override val cause: Throwable? = null,
        override val userMessage: String? = null,
    ) : AppException()

    /** Lỗi ML Kit OCR. */
    data class OcrException(
        val kind: OcrErrorKind,
        override val cause: Throwable? = null,
    ) : AppException()

    /** Lỗi khi đăng nhập / đăng xuất tài khoản Google. */
    data class AuthException(
        val kind: AuthErrorKind,
        override val cause: Throwable? = null,
    ) : AppException()

    /** Lỗi nghiệp vụ có định nghĩa rõ (validate, trạng thái không hợp lệ…). Feature tự tạo class con. */
    abstract class CustomException(override val userMessage: String? = null) : AppException(userMessage)

    /** Lỗi không lường trước — luôn được bọc bởi BaseViewModel. */
    data class UncaughtException(override val cause: Throwable?) : AppException()
}

enum class LocalErrorKind { NOT_FOUND, CONSTRAINT_VIOLATION, DISK_FULL, MIGRATION_FAILED, UNKNOWN }

enum class AiErrorKind {
    QUOTA_EXCEEDED_LOCAL,   // bộ đếm DataStore chặn trước khi gọi
    QUOTA_EXCEEDED_SERVER,  // Google trả 429
    APP_CHECK_FAILED,       // App Check chưa cấu hình / token sai
    NETWORK,                // mất mạng khi gọi AI
    CONTENT_BLOCKED,        // prompt/response bị chặn vì safety
    RESPONSE_PARSE_ERROR,   // JSON trả về sai schema
    EMPTY_RESPONSE,
    MODEL_UNAVAILABLE,
    NOT_CONFIGURED,         // Firebase AI chưa được gắn (Sprint 1 dùng stub)
    UNKNOWN,
}

enum class OcrErrorKind { NO_TEXT_FOUND, RECOGNITION_FAILED }

enum class AuthErrorKind {
    NOT_CONFIGURED,     // bản build không có google-services.json nên không có Firebase
    NETWORK,            // mất mạng lúc đăng nhập
    NO_GOOGLE_ACCOUNT,  // máy chưa có tài khoản Google nào để chọn
    UNKNOWN,
}
