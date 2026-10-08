package com.ledinhthi.ontaptld.feature.capture.data

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import com.ledinhthi.ontaptld.core.di.IoDispatcher
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import com.ledinhthi.ontaptld.core.image.BitmapLoader
import com.ledinhthi.ontaptld.feature.capture.domain.exception.CaptureException
import com.ledinhthi.ontaptld.feature.capture.domain.model.CropRect
import com.ledinhthi.ontaptld.feature.capture.domain.repository.CaptureImageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Giữ ảnh tạm của luồng chụp trong `cacheDir/capture/`. Thư mục cache thuộc riêng app (app khác
 * không đọc được, không cần xin quyền bộ nhớ) và hệ thống được phép tự xoá khi máy đầy.
 */
@Singleton
class CaptureImageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val ids: IdGenerator,
) : CaptureImageRepository {

    private val directory: File get() = File(context.cacheDir, "capture")

    // `withContext(io)`: chuyển việc đọc / ghi file sang luồng nền, xong thì tự quay về luồng gọi.
    override suspend fun newPhotoFile(): String = withContext(io) {
        clearDirectory()
        newFile("photo").absolutePath
    }

    override suspend fun importImage(uri: String): String = withContext(io) {
        clearDirectory()
        val target = newFile("picked")
        try {
            // Ảnh trong thư viện không phải file thường mà là một "địa chỉ nội dung" (content://…);
            // ContentResolver mở nó ra thành dòng byte, ta chép nguyên dòng đó vào file của mình.
            // `use { }` tự đóng dòng byte khi xong, kể cả lúc có lỗi.
            val input = context.contentResolver.openInputStream(uri.toUri())
                ?: throw CaptureException(CaptureException.Kind.IMAGE_UNREADABLE)
            input.use { source -> target.outputStream().use { source.copyTo(it) } }
        } catch (e: IOException) {
            throw CaptureException(CaptureException.Kind.IMAGE_UNREADABLE, e)
        } catch (e: SecurityException) {
            throw CaptureException(CaptureException.Kind.IMAGE_UNREADABLE, e)
        }
        // Thử đọc kích thước ngay: chọn nhầm file không phải ảnh thì báo luôn ở bước này.
        if (BitmapLoader.decodeUpright(target.absolutePath, maxSide = 64) == null) {
            target.delete()
            throw CaptureException(CaptureException.Kind.IMAGE_UNREADABLE)
        }
        target.absolutePath
    }

    override suspend fun cropImage(sourcePath: String, crop: CropRect): String = withContext(io) {
        val source = BitmapLoader.decodeUpright(sourcePath, MaxCropSourceSide)
            ?: throw CaptureException(CaptureException.Kind.IMAGE_UNREADABLE)
        // Đổi khung tỉ lệ (0..1) thành pixel thật của ảnh, và giữ cho nó không tràn khỏi ảnh.
        val left = (crop.left * source.width).roundToInt().coerceIn(0, source.width - 1)
        val top = (crop.top * source.height).roundToInt().coerceIn(0, source.height - 1)
        val width = (crop.width * source.width).roundToInt().coerceIn(1, source.width - left)
        val height = (crop.height * source.height).roundToInt().coerceIn(1, source.height - top)
        val cropped = Bitmap.createBitmap(source, left, top, width, height)

        val target = newFile("crop")
        try {
            target.outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, JpegQuality, it) }
        } catch (e: IOException) {
            target.delete()
            throw CaptureException(CaptureException.Kind.SAVE_FAILED, e)
        } finally {
            if (cropped !== source) cropped.recycle()
            source.recycle()
        }
        target.absolutePath
    }

    override suspend fun keepNoteImage(tempPath: String, noteId: String): String = withContext(io) {
        // `filesDir` khác `cacheDir` ở chỗ hệ thống không bao giờ tự dọn — hợp với ảnh nguồn của
        // thẻ, thứ người dùng còn mở lại xem về sau.
        val notesDirectory = File(context.filesDir, "notes").apply { mkdirs() }
        val target = File(notesDirectory, "$noteId.jpg")
        // CHÉP chứ không chuyển: nếu bước lưu thẻ phía sau hỏng, ảnh tạm vẫn còn để bấm lưu lại.
        // Ảnh tạm sẽ tự được dọn ở lần chụp kế tiếp.
        try {
            File(tempPath).copyTo(target, overwrite = true)
        } catch (e: NoSuchFileException) {
            // File tạm đã bị hệ thống dọn mất (máy đầy) trong lúc người dùng còn đang duyệt thẻ.
            // Phải bắt lỗi này TRƯỚC IOException vì nó là một loại IOException cụ thể hơn.
            throw CaptureException(CaptureException.Kind.IMAGE_UNREADABLE, e)
        } catch (e: IOException) {
            target.delete()
            throw CaptureException(CaptureException.Kind.SAVE_FAILED, e)
        }
        target.absolutePath
    }

    override suspend fun deleteNoteImage(path: String): Unit = withContext(io) {
        File(path).delete()
    }

    private fun newFile(prefix: String): File {
        directory.mkdirs()
        return File(directory, "$prefix-${ids.newId()}.jpg")
    }

    /**
     * Xoá ảnh của lần chụp trước. An toàn vì luồng đi một chiều (chụp → kiểm tra chữ → tạo thẻ):
     * đã quay về chụp ảnh mới thì không màn nào còn dùng tới ảnh cũ.
     */
    private fun clearDirectory() {
        directory.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        /** Cạnh dài tối đa của ảnh đem cắt — đủ nét cho nhận dạng chữ mà không ngốn RAM. */
        const val MaxCropSourceSide = 2560
        const val JpegQuality = 92
    }
}
