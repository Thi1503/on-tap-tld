package com.ledinhthi.ontaptld.feature.capture.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.OcrErrorKind
import com.ledinhthi.ontaptld.feature.capture.domain.repository.OcrRepository
import javax.inject.Inject

/**
 * Nhận dạng chữ trong ảnh ghi chú đã cắt; trả về văn bản đã bỏ khoảng trắng thừa ở hai đầu.
 * Ảnh không có chữ nào thì ném `OcrException(NO_TEXT_FOUND)` — với nơi gọi, "không có gì để
 * tạo thẻ" là một tình huống phải xử lý riêng chứ không phải một kết quả bình thường.
 */
class RunOcrUseCase @Inject constructor(
    private val repository: OcrRepository,
) : UseCase<String, String>() {
    override suspend fun invoke(input: String): String {
        val text = repository.recognizeText(input).trim()
        if (text.isEmpty()) throw AppException.OcrException(OcrErrorKind.NO_TEXT_FOUND)
        return text
    }
}
