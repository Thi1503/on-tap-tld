package com.ledinhthi.ontaptld.feature.capture.domain.repository

/** Nhận dạng chữ trong ảnh (OCR). Bản cài đặt dùng ML Kit, chạy ngay trên máy. */
interface OcrRepository {
    /**
     * Trả về toàn bộ chữ đọc được trong ảnh ở [imagePath], mỗi dòng chữ một dòng. Chuỗi rỗng
     * nếu ảnh không có chữ. Ném `AppException.OcrException` khi bộ nhận dạng gặp lỗi.
     */
    suspend fun recognizeText(imagePath: String): String
}
