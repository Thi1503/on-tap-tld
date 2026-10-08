package com.ledinhthi.ontaptld.feature.settings.data

import com.ledinhthi.ontaptld.core.data.local.db.AppDatabase
import com.ledinhthi.ontaptld.core.data.local.db.wrapLocal
import com.ledinhthi.ontaptld.core.di.IoDispatcher
import com.ledinhthi.ontaptld.feature.capture.domain.repository.CaptureImageRepository
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteImageStore
import com.ledinhthi.ontaptld.feature.settings.domain.LocalDataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalDataRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val noteImages: NoteImageStore,
    private val captureImages: CaptureImageRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LocalDataRepository {

    override suspend fun deleteAll() {
        // `clearAllTables()` xoá sạch mọi bảng trong MỘT transaction, rồi báo cho những nơi đang
        // nghe database (Home, Chi tiết bộ thẻ…) để chúng tự vẽ lại. Nó là hàm chặn luồng (không
        // phải `suspend`) nên phải tự đẩy sang luồng nền.
        withContext(io) { wrapLocal { database.clearAllTables() } }
        // Ảnh nằm ngoài database -> xoá riêng, và chỉ sau khi database đã xoá xong.
        noteImages.deleteAll()
        captureImages.clearTempImages()
    }
}
