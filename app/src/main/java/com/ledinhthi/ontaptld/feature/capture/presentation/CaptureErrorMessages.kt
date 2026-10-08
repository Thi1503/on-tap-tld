package com.ledinhthi.ontaptld.feature.capture.presentation

import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.text.StringProvider
import com.ledinhthi.ontaptld.feature.capture.domain.exception.CaptureException

/** Thông báo hiển thị (đã dịch) cho lỗi của luồng chụp ghi chú — domain chỉ mang `kind`. */
fun CaptureException.displayMessage(strings: StringProvider): String = strings.get(
    when (kind) {
        CaptureException.Kind.IMAGE_UNREADABLE -> R.string.capture_error_image
        CaptureException.Kind.SAVE_FAILED -> R.string.capture_error_save
    },
)
