package com.ledinhthi.ontaptld.feature.capture.domain.repository

import com.ledinhthi.ontaptld.feature.capture.domain.model.CropRect

/**
 * Kho file ảnh TẠM của luồng chụp ghi chú. Mọi file nằm trong thư mục cache của app; ảnh chỉ
 * được chuyển sang chỗ lưu lâu dài khi người dùng lưu thẻ ở cuối luồng.
 *
 * Mọi hàm nhận / trả ĐƯỜNG DẪN file (String) chứ không trả ảnh, để tầng domain và ViewModel
 * không dính tới các lớp của Android (Bitmap, Uri) và test được bằng unit test thường.
 */
interface CaptureImageRepository {
    /** Dọn ảnh tạm của lần chụp trước rồi trả về đường dẫn cho camera ghi ảnh mới vào. */
    suspend fun newPhotoFile(): String

    /** Dọn ảnh tạm cũ, chép ảnh người dùng chọn ([uri] dạng chuỗi) vào app, trả về đường dẫn. */
    suspend fun importImage(uri: String): String

    /** Cắt ảnh [sourcePath] theo khung [crop], ghi ra file mới và trả về đường dẫn file đó. */
    suspend fun cropImage(sourcePath: String, crop: CropRect): String

    /**
     * Chép ảnh tạm [tempPath] sang chỗ lưu lâu dài của ghi chú [noteId] (thư mục riêng của app,
     * hệ thống không tự xoá) và trả về đường dẫn mới. Gọi khi người dùng lưu thẻ.
     */
    suspend fun keepNoteImage(tempPath: String, noteId: String): String

    /** Xoá ảnh ghi chú đã lưu lâu dài — dùng để dọn dẹp khi việc lưu thẻ hỏng giữa chừng. */
    suspend fun deleteNoteImage(path: String)
}
