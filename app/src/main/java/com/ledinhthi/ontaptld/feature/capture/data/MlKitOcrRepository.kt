package com.ledinhthi.ontaptld.feature.capture.data

import android.content.Context
import android.graphics.Rect
import androidx.core.net.toUri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ledinhthi.ontaptld.core.di.IoDispatcher
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.OcrErrorKind
import com.ledinhthi.ontaptld.feature.capture.domain.model.OcrLine
import com.ledinhthi.ontaptld.feature.capture.domain.repository.OcrRepository
import com.ledinhthi.ontaptld.feature.deck.domain.model.SourceBox
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Nhận dạng chữ bằng ML Kit Text Recognition, bản GÓI KÈM APP: mô hình nằm sẵn trong APK nên
 * chạy được ngay cả khi không có mạng, và ảnh không rời khỏi máy. Bộ nhận dạng chữ Latin đọc
 * được tiếng Việt có dấu.
 */
@Singleton
class MlKitOcrRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : OcrRepository {

    // `by lazy`: chỉ tạo bộ nhận dạng (khá nặng) ở lần đầu thật sự cần, rồi dùng lại mãi.
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun recognizeLines(imagePath: String): List<OcrLine> {
        // Đọc file ảnh là việc chậm nên làm ở luồng nền.
        val image = withContext(io) {
            try {
                InputImage.fromFilePath(context, File(imagePath).toUri())
            } catch (e: IOException) {
                throw AppException.OcrException(OcrErrorKind.RECOGNITION_FAILED, e)
            }
        }
        // Kích thước ảnh ĐÃ dựng thẳng: ML Kit báo vị trí chữ theo ảnh sau khi xoay cho đúng
        // chiều, nên ảnh bị ghi nghiêng 90° / 270° thì chiều rộng và chiều cao đổi chỗ cho nhau.
        val sideways = image.rotationDegrees % 180 != 0
        val width = if (sideways) image.height else image.width
        val height = if (sideways) image.width else image.height
        // ML Kit trả kết quả kiểu "gọi lại sau" (listener). `suspendCancellableCoroutine` là cầu
        // nối sang coroutine: hàm tạm dừng ở đây, tới khi một trong hai listener được gọi thì
        // chạy tiếp với kết quả (`resume`) hoặc ném lỗi (`resumeWithException`).
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    // ML Kit chia chữ thành từng KHỐI (đoạn), mỗi khối gồm nhiều DÒNG, nhưng không
                    // cam kết thứ tự các khối. Xếp lại theo vị trí trên ảnh — từ trên xuống, cùng
                    // hàng thì trái trước — để văn bản ra đúng thứ tự người đọc một trang ghi chú.
                    // `flatMap` gộp các dòng của mọi khối thành một danh sách liền.
                    val lines = result.textBlocks
                        .sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
                        .flatMap { block -> block.lines }
                        .filter { it.text.isNotBlank() }
                        .map { line -> OcrLine(text = line.text, box = line.boundingBox?.toSourceBox(width, height)) }
                    continuation.resume(lines)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(
                        AppException.OcrException(OcrErrorKind.RECOGNITION_FAILED, e),
                    )
                }
        }
    }

    /**
     * Đổi khung tính bằng pixel của ML Kit sang khung tính theo tỉ lệ của ảnh (0..1), để lưu
     * cùng thẻ mà không phụ thuộc ảnh được hiển thị ở cỡ nào. null nếu không biết cỡ ảnh.
     */
    private fun Rect.toSourceBox(imageWidth: Int, imageHeight: Int): SourceBox? {
        if (imageWidth <= 0 || imageHeight <= 0) return null
        return SourceBox(
            left = (left.toFloat() / imageWidth).coerceIn(0f, 1f),
            top = (top.toFloat() / imageHeight).coerceIn(0f, 1f),
            right = (right.toFloat() / imageWidth).coerceIn(0f, 1f),
            bottom = (bottom.toFloat() / imageHeight).coerceIn(0f, 1f),
        )
    }
}
