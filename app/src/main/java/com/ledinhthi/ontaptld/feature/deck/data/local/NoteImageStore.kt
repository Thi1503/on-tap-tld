package com.ledinhthi.ontaptld.feature.deck.data.local

import android.content.Context
import com.ledinhthi.ontaptld.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lo việc XOÁ file ảnh của ghi chú trong `filesDir/notes/`. (Việc chép ảnh vào thư mục này nằm
 * ở `CaptureImageRepositoryImpl.keepNoteImage`, bước lưu thẻ AI.)
 *
 * Database chỉ giữ đường dẫn tới ảnh. Xoá dòng trong database không làm file biến mất, nên mỗi
 * chỗ xoá ghi chú phải gọi sang đây, nếu không ảnh nằm lại trên máy tới khi gỡ app.
 */
@Singleton
class NoteImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val directory: File get() = File(context.filesDir, DIRECTORY)

    suspend fun delete(paths: Collection<String>): Unit = withContext(io) {
        paths.forEach { File(it).delete() }
    }

    /**
     * Xoá mọi ảnh trong thư mục mà không ghi chú nào còn dùng. [keepPaths] = đường dẫn ảnh của
     * các ghi chú còn sống. Ảnh sửa lần cuối từ [modifiedBefore] trở về sau được chừa lại: có
     * thể nó vừa được chép vào và ghi chú của nó chưa kịp ghi xuống database.
     *
     * Trả về số file đã xoá.
     */
    suspend fun deleteUnreferenced(keepPaths: Collection<String>, modifiedBefore: Long): Int = withContext(io) {
        // So theo TÊN file (`<noteId>.jpg`) thay vì cả đường dẫn: cùng một thư mục của app có thể
        // được hệ thống viết theo hai kiểu (/data/data/… và /data/user/0/…).
        val keepNames = keepPaths.mapTo(HashSet()) { File(it).name }
        // `listFiles()` trả null khi thư mục chưa tồn tại (chưa lưu thẻ AI nào) -> coi như rỗng.
        directory.listFiles().orEmpty().count { file ->
            file.isFile && file.name !in keepNames && file.lastModified() < modifiedBefore && file.delete()
        }
    }

    companion object {
        /** Tên thư mục con trong `filesDir` chứa ảnh ghi chú. */
        const val DIRECTORY = "notes"
    }
}
