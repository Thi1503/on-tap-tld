package com.ledinhthi.ontaptld.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import timber.log.Timber

/**
 * Đọc file ảnh thành [Bitmap] để hiển thị hoặc xử lý. Lo sẵn hai việc mà đọc "thô" bằng
 * `BitmapFactory.decodeFile` không làm:
 *
 *  1. THU NHỎ lúc đọc. Ảnh camera 12 MP mở nguyên cỡ tốn gần 50 MB RAM; màn hình và OCR đều
 *     không cần nhiều điểm ảnh đến thế.
 *  2. DỰNG THẲNG. Nhiều máy lưu ảnh nằm ngang rồi ghi chú "xoay 90°" trong phần thông tin EXIF
 *     của file. Bỏ qua ghi chú đó thì ảnh hiện ra bị nằm nghiêng.
 *
 * Các hàm ở đây đọc file nên chạy chậm — luôn gọi từ luồng nền (`Dispatchers.IO`).
 */
object BitmapLoader {

    /**
     * Đọc ảnh ở [path], đã dựng thẳng, cạnh dài không quá [maxSide] pixel.
     * Trả về null nếu file không tồn tại hoặc không phải ảnh.
     */
    fun decodeUpright(path: String, maxSide: Int): Bitmap? = try {
        // Lượt 1: `inJustDecodeBounds` chỉ đọc kích thước, chưa nạp điểm ảnh nào vào RAM.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            // Lượt 2: đọc thật. `inSampleSize = 2` nghĩa là cứ 2 điểm lấy 1 (ảnh nhỏ đi một nửa
            // mỗi chiều); Android chỉ nhận luỹ thừa của 2 nên ta nhân đôi dần cho tới khi vừa.
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, options)?.let { rotateByExif(it, path) }
        }
    } catch (e: Exception) {
        Timber.w(e, "Không đọc được ảnh")
        null
    } catch (e: OutOfMemoryError) {
        Timber.w(e, "Ảnh quá lớn so với bộ nhớ còn trống")
        null
    }

    private fun rotateByExif(bitmap: Bitmap, path: String): Bitmap {
        val orientation = ExifInterface(path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        // Matrix = "công thức biến hình": xoay, lật… rồi áp cho cả tấm ảnh trong một lần.
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }

            else -> return bitmap // ảnh vốn đã thẳng
        }
        val upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (upright !== bitmap) bitmap.recycle() // trả RAM của bản chưa xoay
        return upright
    }
}
