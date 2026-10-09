package com.ledinhthi.ontaptld.core.sync

import com.ledinhthi.ontaptld.core.domain.util.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Xin chạy một lượt đồng bộ ở nền. Phần hiện thực (WorkManager) nằm ở `feature/sync`. */
interface SyncScheduler {
    /** Hẹn một lượt đồng bộ sớm nhất có thể (khi có mạng). Chưa đăng nhập thì không làm gì. */
    fun requestSync()

    fun cancel()
}

/**
 * Cửa ghi lệnh vào hàng đợi đồng bộ. Các repository của bộ thẻ / thẻ / ghi chú gọi vào đây NGAY
 * SAU mỗi lần ghi xuống database, để thay đổi đó về sau được đẩy lên đám mây.
 *
 * Lệnh được ghi cả khi chưa đăng nhập: tới lúc đăng nhập, hàng đợi đã có sẵn việc phải làm.
 */
@Singleton
class SyncQueueRecorder @Inject constructor(
    private val dao: SyncDao,
    private val clock: Clock,
    private val scheduler: SyncScheduler,
) {
    /** Các dòng [ids] của bảng [type] vừa được tạo, sửa hoặc đánh dấu xoá. */
    suspend fun recordChanged(type: SyncEntityType, ids: List<String>) {
        if (ids.isEmpty()) return
        val now = clock.nowMillis()
        // SQLite giới hạn số tham số trong một câu lệnh, nên danh sách dài được cắt thành khúc.
        ids.chunked(MAX_IDS_PER_QUERY).forEach { chunk ->
            when (type) {
                SyncEntityType.DECK -> dao.enqueueDecks(chunk, now)
                SyncEntityType.NOTE -> dao.enqueueNotes(chunk, now)
                SyncEntityType.FLASHCARD -> dao.enqueueFlashcards(chunk, now)
            }
        }
        scheduler.requestSync()
    }

    /**
     * Vừa có dòng bị đổi GIÁN TIẾP mà nơi gọi không biết id (xoá bộ thẻ kéo theo các thẻ và ghi
     * chú bên trong, dọn ghi chú mồ côi…): quét và ghi lệnh cho mọi dòng chưa có lệnh.
     */
    suspend fun recordCascades() {
        dao.enqueueUnsynced(clock.nowMillis())
        scheduler.requestSync()
    }

    private companion object {
        const val MAX_IDS_PER_QUERY = 500
    }
}
