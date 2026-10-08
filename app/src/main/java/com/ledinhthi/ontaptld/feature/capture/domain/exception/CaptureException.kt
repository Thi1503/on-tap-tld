package com.ledinhthi.ontaptld.feature.capture.domain.exception

import com.ledinhthi.ontaptld.core.exception.AppException

/** Lỗi của luồng chụp ghi chú. Câu chữ hiện cho người dùng nằm ở `CaptureErrorMessages`. */
class CaptureException(val kind: Kind, override val cause: Throwable? = null) : AppException.CustomException() {
    enum class Kind {
        /** File không phải ảnh, hoặc ảnh hỏng không đọc được. */
        IMAGE_UNREADABLE,

        /** Không ghi được file ảnh (thường do máy hết chỗ). */
        SAVE_FAILED,
    }
}
