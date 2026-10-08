package com.ledinhthi.ontaptld.feature.sync.data.remote

import com.ledinhthi.ontaptld.core.sync.SyncEntityType

/** Một bản ghi trên đám mây (đọc về hoặc sắp ghi lên): thuộc collection nào, id gì, gồm những trường nào. */
data class RemoteDoc(
    val type: SyncEntityType,
    val id: String,
    val fields: Map<String, Any?>,
)

/**
 * Kết quả một lần kéo: các bản ghi đổi kể từ mốc cũ, và [cursor] = mốc mới để dùng cho lần sau
 * (nano giây theo giờ máy chủ).
 */
data class RemoteChanges(
    val docs: List<RemoteDoc>,
    val cursor: Long,
)

/**
 * Phần nói chuyện với đám mây, tách thành interface để logic đồng bộ (`SyncRepositoryImpl`)
 * không phụ thuộc Firestore và test được bằng một bản giả. Mọi lỗi được đổi sang
 * `AppException.SyncException` trước khi ra khỏi đây.
 */
interface SyncRemoteDataSource {
    /** Các bản ghi của [uid] trong collection [type] được ghi lên đám mây SAU mốc [cursor]. */
    suspend fun fetchChangedSince(uid: String, type: SyncEntityType, cursor: Long): RemoteChanges

    /** Ghi cả lô [docs] lên đám mây của [uid]: hoặc tất cả cùng thành công, hoặc không có gì đổi. */
    suspend fun upload(uid: String, docs: List<RemoteDoc>)
}
