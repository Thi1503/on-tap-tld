package com.ledinhthi.ontaptld.feature.sync.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.Source
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.SyncErrorKind
import com.ledinhthi.ontaptld.core.sync.SyncEntityType
import com.ledinhthi.ontaptld.core.sync.collectionName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Đám mây là Cloud Firestore, cấu trúc `users/{uid}/{decks|notes|flashcards}/{id}` (docs_tld Mục
 * 7.1). Security Rules chỉ cho mỗi người đọc / ghi phần dưới uid của chính mình.
 *
 * Mỗi bản ghi được ghi kèm trường [SYNCED_AT] = giờ MÁY CHỦ lúc ghi. Lần kéo sau chỉ hỏi "những
 * gì có syncedAt lớn hơn mốc lần trước". Không dùng `updatedAt` cho việc này vì đó là giờ của
 * từng máy: một máy mất mạng cả buổi rồi mới đẩy lên thì `updatedAt` của nó đã cũ hơn mốc kéo
 * của máy khác, và thay đổi đó sẽ không bao giờ được kéo về. (`updatedAt` vẫn là thứ quyết định
 * bản nào thắng khi hai máy cùng sửa.)
 */
@Singleton
class FirestoreSyncDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : SyncRemoteDataSource {

    // `by lazy`: chỉ dựng ở lần dùng đầu, khi đã chắc có Firebase.
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            // Tắt kho đệm trên đĩa của Firestore: bản offline của app đã là Room, giữ thêm một
            // bản nữa chỉ tốn chỗ và dễ lệch nhau.
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        }
    }

    override suspend fun fetchChangedSince(uid: String, type: SyncEntityType, cursor: Long): RemoteChanges =
        guarded {
            val docs = mutableListOf<RemoteDoc>()
            var newCursor = cursor
            var lastOfPage: DocumentSnapshot? = null
            // Đọc từng trang để một lần kéo lớn (lần đầu trên máy mới) không dồn vào một phản hồi.
            do {
                var query = collection(uid, type)
                    .whereGreaterThan(SYNCED_AT, cursor.toTimestamp())
                    .orderBy(SYNCED_AT)
                    .limit(PAGE_SIZE)
                // `startAfter(bản ghi cuối của trang trước)`: tiếp đúng chỗ vừa dừng, kể cả khi
                // nhiều bản ghi có cùng syncedAt (cả một lô được ghi cùng lúc).
                lastOfPage?.let { query = query.startAfter(it) }
                // Source.SERVER: bắt buộc hỏi máy chủ; mất mạng thì báo lỗi ngay, không trả bản đệm.
                val page = query.get(Source.SERVER).await().documents
                page.forEach { doc ->
                    doc.data?.let { fields -> docs += RemoteDoc(type, doc.id, fields) }
                    doc.getTimestamp(SYNCED_AT)?.let { newCursor = maxOf(newCursor, it.toNanos()) }
                }
                lastOfPage = page.lastOrNull()
            } while (page.size >= PAGE_SIZE)
            RemoteChanges(docs, newCursor)
        }

    override suspend fun upload(uid: String, docs: List<RemoteDoc>) {
        if (docs.isEmpty()) return
        guarded {
            // "Batch": gom nhiều lệnh ghi thành một lần gửi; Firestore ghi tất cả hoặc không ghi gì.
            val batch = firestore.batch()
            docs.forEach { doc ->
                val fields = doc.fields + (SYNCED_AT to FieldValue.serverTimestamp())
                batch.set(collection(uid, doc.type).document(doc.id), fields)
            }
            batch.commit().await()
        }
    }

    private fun collection(uid: String, type: SyncEntityType): CollectionReference =
        firestore.collection(USERS).document(uid).collection(type.collectionName)

    /** Chạy một lệnh Firestore có giới hạn thời gian, và đổi lỗi của thư viện sang lỗi của app. */
    private suspend fun <T> guarded(block: suspend () -> T): T {
        // Máy build không có google-services.json thì Firebase không được khởi tạo (xem OnTapTldApp).
        if (FirebaseApp.getApps(context).isEmpty()) {
            throw AppException.SyncException(SyncErrorKind.NOT_CONFIGURED)
        }
        return try {
            // Mất mạng thì lệnh GHI của Firestore không báo lỗi mà đứng chờ tới khi có mạng lại.
            // `withTimeout` cắt việc chờ sau một lúc bằng cách ném TimeoutCancellationException.
            withTimeout(TIMEOUT_MILLIS) { block() }
        } catch (e: TimeoutCancellationException) {
            // Phải bắt TRƯỚC nhánh CancellationException bên dưới (nó là một loại con của lỗi đó):
            // hết giờ chờ là "mạng có vấn đề", không phải "việc này bị huỷ".
            throw AppException.SyncException(SyncErrorKind.NETWORK, e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            throw AppException.SyncException(e.code.toSyncErrorKind(), e)
        }
    }

    private companion object {
        const val USERS = "users"
        const val SYNCED_AT = "syncedAt"
        const val PAGE_SIZE = 300L
        const val TIMEOUT_MILLIS = 30_000L
    }
}

private fun FirebaseFirestoreException.Code.toSyncErrorKind(): SyncErrorKind = when (this) {
    // Lỗi tạm thời: lát nữa thử lại là được.
    FirebaseFirestoreException.Code.UNAVAILABLE,
    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
    FirebaseFirestoreException.Code.ABORTED,
    FirebaseFirestoreException.Code.CANCELLED,
    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
    -> SyncErrorKind.NETWORK

    FirebaseFirestoreException.Code.PERMISSION_DENIED,
    FirebaseFirestoreException.Code.UNAUTHENTICATED,
    -> SyncErrorKind.PERMISSION_DENIED

    else -> SyncErrorKind.UNKNOWN
}

// Mốc kéo lưu dưới dạng MỘT số Long = số nano giây kể từ 1/1/1970. Giữ tới nano giây (không làm
// tròn về mili giây) để lần kéo sau không lấy lại chính các bản ghi vừa kéo.
private const val NANOS_PER_SECOND = 1_000_000_000L

private fun Timestamp.toNanos(): Long = seconds * NANOS_PER_SECOND + nanoseconds

private fun Long.toTimestamp(): Timestamp = Timestamp(this / NANOS_PER_SECOND, (this % NANOS_PER_SECOND).toInt())
