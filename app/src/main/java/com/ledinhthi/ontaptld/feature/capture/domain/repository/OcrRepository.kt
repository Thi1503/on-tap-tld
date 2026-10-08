package com.ledinhthi.ontaptld.feature.capture.domain.repository

import com.ledinhthi.ontaptld.feature.capture.domain.model.OcrLine

/** Nhận dạng chữ trong ảnh (OCR). Bản cài đặt dùng ML Kit, chạy ngay trên máy. */
interface OcrRepository {
    /**
     * Trả về các dòng chữ đọc được trong ảnh ở [imagePath], theo thứ tự đọc (trên xuống, trái
     * sang phải), mỗi dòng kèm vị trí trên ảnh. Danh sách rỗng nếu ảnh không có chữ. Ném
     * `AppException.OcrException` khi bộ nhận dạng gặp lỗi.
     */
    suspend fun recognizeLines(imagePath: String): List<OcrLine>
}
