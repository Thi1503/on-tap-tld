package com.ledinhthi.ontaptld.feature.sync.domain

import kotlinx.coroutines.flow.Flow

/**
 * Trạng thái đồng bộ để màn Cài đặt hiển thị.
 *
 * @param isSyncing đang có một lượt đồng bộ chạy.
 * @param lastFailed lượt gần nhất (kể từ lúc mở app) bị lỗi.
 * @param lastSyncedAtMillis lần đồng bộ THÀNH CÔNG gần nhất; null = chưa lần nào.
 */
data class SyncStatus(
    val isSyncing: Boolean = false,
    val lastFailed: Boolean = false,
    val lastSyncedAtMillis: Long? = null,
)

/** Đồng bộ hai chiều giữa database trên máy và đám mây của người đang đăng nhập (docs_tld Mục 8.5). */
interface SyncRepository {
    val status: Flow<SyncStatus>

    /**
     * Chạy một lượt: kéo thay đổi từ đám mây về, rồi đẩy thay đổi của máy lên. Đang có lượt khác
     * chạy thì chờ nó xong rồi mới chạy. Lỗi thì ném `AppException.SyncException`.
     */
    suspend fun sync()

    /**
     * Cắt liên hệ giữa dữ liệu trên máy và tài khoản đang đồng bộ: quên mốc kéo, giờ đồng bộ và
     * "chủ" của dữ liệu. Chờ lượt đồng bộ đang chạy (nếu có) kết thúc rồi mới làm.
     */
    suspend fun forgetAccount()
}
