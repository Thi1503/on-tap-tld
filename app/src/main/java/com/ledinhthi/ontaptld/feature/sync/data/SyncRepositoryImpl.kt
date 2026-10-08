package com.ledinhthi.ontaptld.feature.sync.data

import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.di.IoDispatcher
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.SyncErrorKind
import com.ledinhthi.ontaptld.core.sync.PushedCommand
import com.ledinhthi.ontaptld.core.sync.SyncDao
import com.ledinhthi.ontaptld.core.sync.SyncEntityType
import com.ledinhthi.ontaptld.core.sync.SyncQueueDao
import com.ledinhthi.ontaptld.core.sync.SyncQueueEntity
import com.ledinhthi.ontaptld.feature.auth.domain.AuthRepository
import com.ledinhthi.ontaptld.feature.sync.data.remote.RemoteDoc
import com.ledinhthi.ontaptld.feature.sync.data.remote.SyncRemoteDataSource
import com.ledinhthi.ontaptld.feature.sync.data.remote.toDeckEntity
import com.ledinhthi.ontaptld.feature.sync.data.remote.toFlashcardEntity
import com.ledinhthi.ontaptld.feature.sync.data.remote.toNoteEntity
import com.ledinhthi.ontaptld.feature.sync.data.remote.toRemoteFields
import com.ledinhthi.ontaptld.feature.sync.domain.SyncRepository
import com.ledinhthi.ontaptld.feature.sync.domain.SyncStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bộ máy đồng bộ (docs_tld Mục 8.5). Một lượt gồm bốn việc, theo thứ tự:
 *
 * 1. **Nhận dữ liệu** — lần đầu máy này đồng bộ với một tài khoản thì mọi thứ đang có trên máy
 *    được xếp vào hàng đợi (Mục 8.6).
 * 2. **Kéo về** — hỏi đám mây những bản ghi đổi kể từ lần trước, trộn vào máy theo luật
 *    Last-Write-Wins: bản nào có `updatedAt` lớn hơn thì thắng.
 * 3. **Đẩy lên** — đọc lần lượt `sync_queue`, ghi các dòng tương ứng lên đám mây, xong thì đánh
 *    dấu `synced = 1` và rút lệnh khỏi hàng đợi.
 * 4. Ghi lại giờ đồng bộ để màn Cài đặt hiển thị.
 *
 * Kéo TRƯỚC, đẩy SAU: nếu đẩy trước, một bản sửa cũ nằm chờ trên máy sẽ đè lên bản mới hơn mà
 * máy khác đã đưa lên. Kéo trước thì bản mới hơn kịp thắng (và lệnh đang chờ của bản cũ bị bỏ).
 *
 * `@Singleton`: cả app dùng chung một bộ máy, nên nút "Đồng bộ ngay" và việc nền không bao giờ
 * chạy chồng lên nhau, và ai gọi thì màn Cài đặt cũng thấy trạng thái "Đang đồng bộ…".
 */
@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val auth: AuthRepository,
    private val syncDao: SyncDao,
    private val queueDao: SyncQueueDao,
    private val remote: SyncRemoteDataSource,
    private val prefs: AppPreferences,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SyncRepository {

    // Mutex = "khoá": mỗi lúc chỉ một coroutine được ở trong `withLock { … }`, các coroutine
    // khác đứng chờ tới lượt.
    private val mutex = Mutex()
    private val running = MutableStateFlow(false)
    private val lastRunFailed = MutableStateFlow(false)

    // `combine` gộp ba dòng dữ liệu thành một: cứ một trong ba đổi là phát một SyncStatus mới.
    override val status: Flow<SyncStatus> =
        combine(running, lastRunFailed, prefs.lastSyncAtMillis) { isSyncing, failed, lastSyncAt ->
            SyncStatus(
                isSyncing = isSyncing,
                lastFailed = failed,
                lastSyncedAtMillis = lastSyncAt.takeIf { it > 0 },
            )
        }

    override suspend fun sync() {
        val uid = auth.currentUserId ?: throw AppException.SyncException(SyncErrorKind.NOT_SIGNED_IN)
        mutex.withLock {
            running.value = true
            try {
                withContext(io) { runOnce(uid) }
                lastRunFailed.value = false
            } catch (e: CancellationException) {
                throw e // bị huỷ giữa chừng không phải là lỗi; lượt sau làm lại từ đầu vẫn đúng
            } catch (e: AppException) {
                lastRunFailed.value = true
                throw e
            } catch (e: Exception) {
                lastRunFailed.value = true
                throw AppException.SyncException(SyncErrorKind.UNKNOWN, e)
            } finally {
                running.value = false
            }
        }
    }

    override suspend fun forgetAccount() = mutex.withLock {
        prefs.clearSyncState()
        lastRunFailed.value = false
    }

    private suspend fun runOnce(uid: String) {
        claimLocalData(uid)
        val pulled = pull(uid)
        // Thẻ / ghi chú còn sống trong một bộ thẻ vừa bị máy khác xoá: xoá theo cho khớp.
        syncDao.softDeleteContentsOfDeletedDecks(clock.nowMillis())
        val pushed = push(uid)
        prefs.setLastSyncAt(clock.nowMillis())
        // Lọc chữ "Đồng bộ xong" trong Logcat để xem mỗi lượt đã làm gì.
        Timber.d("Đồng bộ xong: kéo về %d bản ghi (ghi vào máy %d), đẩy lên %d", pulled.fetched, pulled.written, pushed)
    }

    // ---------------------------------------------------------------------------------------
    // 1. Nhận dữ liệu trên máy về tài khoản đang đăng nhập
    // ---------------------------------------------------------------------------------------

    private suspend fun claimLocalData(uid: String) {
        val now = clock.nowMillis()
        if (prefs.getSyncOwnerUid() != uid) {
            // Lần đầu đồng bộ, hoặc người dùng đổi sang tài khoản khác: dữ liệu trên máy đi theo
            // tài khoản đang đăng nhập, và phải kéo lại toàn bộ từ đầu.
            syncDao.claimAllFor(uid, now)
            prefs.startSyncFor(uid)
        } else {
            syncDao.assignDecksTo(uid)
            // Lưới an toàn: dòng nào chưa đồng bộ mà chưa có lệnh thì bổ sung.
            syncDao.enqueueUnsynced(now)
        }
    }

    // ---------------------------------------------------------------------------------------
    // 2. Kéo về
    // ---------------------------------------------------------------------------------------

    /** Số bản ghi đám mây trả về, và trong đó bao nhiêu bản thắng (được ghi vào máy). */
    private data class PullSummary(val fetched: Int, val written: Int)

    private suspend fun pull(uid: String): PullSummary {
        var fetched = 0
        var written = 0
        // Bộ thẻ trước, rồi ghi chú, rồi thẻ: thứ được trỏ tới về trước thứ trỏ tới nó.
        SyncEntityType.entries.forEach { type ->
            val changes = remote.fetchChangedSince(uid, type, prefs.getSyncCursor(type.name))
            fetched += changes.docs.size
            changes.docs.chunked(MERGE_CHUNK).forEach { chunk -> written += merge(type, chunk, uid) }
            // Chỉ dời mốc SAU khi đã trộn xong: lỡ hỏng giữa chừng thì lần sau kéo lại đúng chỗ.
            prefs.setSyncCursor(type.name, changes.cursor)
        }
        return PullSummary(fetched, written)
    }

    /** Trả về số bản ghi đã ghi vào máy. */
    private suspend fun merge(type: SyncEntityType, docs: List<RemoteDoc>, uid: String): Int =
        // `mapNotNull`: bản ghi hỏng (thiếu trường, sai kiểu) cho ra null và bị bỏ qua.
        when (type) {
            SyncEntityType.DECK ->
                syncDao.mergePulledDecks(docs.mapNotNull { it.fields.toDeckEntity(it.id, uid) })

            SyncEntityType.NOTE ->
                syncDao.mergePulledNotes(docs.mapNotNull { it.fields.toNoteEntity(it.id) })

            SyncEntityType.FLASHCARD ->
                syncDao.mergePulledFlashcards(docs.mapNotNull { it.fields.toFlashcardEntity(it.id) })
        }

    // ---------------------------------------------------------------------------------------
    // 3. Đẩy lên
    // ---------------------------------------------------------------------------------------

    /** Trả về số lệnh đã đẩy lên được. */
    private suspend fun push(uid: String): Int {
        val failedThisRun = mutableListOf<String>()
        var pushed = 0
        while (true) {
            val commands = queueDao.nextBatch(PUSH_BATCH, MAX_RETRY, failedThisRun)
            if (commands.isEmpty()) return pushed
            val batch = prepare(commands)
            if (batch.isEmpty()) continue // cả lô đều là lệnh thừa, đã dọn — lấy lô kế
            try {
                remote.upload(uid, batch.map { it.doc })
                syncDao.markPushed(batch.map { it.pushed })
                pushed += batch.size
            } catch (e: AppException.SyncException) {
                // Mất mạng, bị từ chối quyền…: lỗi của cả đường truyền chứ không phải của một
                // lệnh nào — dừng lượt này, hàng đợi còn nguyên cho lượt sau.
                if (e.kind != SyncErrorKind.UNKNOWN) throw e
                // Lỗi lạ: có thể chỉ MỘT bản ghi trong lô có vấn đề. Đẩy lại từng lệnh một để
                // các lệnh lành vẫn đi được, lệnh hỏng thì ghi nhận và để lại.
                val failed = pushOneByOne(uid, batch)
                failedThisRun += failed
                pushed += batch.size - failed.size
            }
        }
    }

    /** Trả về id các lệnh đẩy không được. */
    private suspend fun pushOneByOne(uid: String, batch: List<Outgoing>): List<String> {
        val failed = mutableListOf<String>()
        batch.forEach { item ->
            try {
                remote.upload(uid, listOf(item.doc))
                syncDao.markPushed(listOf(item.pushed))
            } catch (e: AppException.SyncException) {
                if (e.kind != SyncErrorKind.UNKNOWN) throw e
                // Tăng số lần hỏng; quá MAX_RETRY lần thì lệnh bị bỏ qua ở các lượt sau, để một
                // lệnh hỏng không chặn cả hàng đợi.
                queueDao.markFailed(item.pushed.command.id, e.cause?.message)
                failed += item.pushed.command.id
            }
        }
        return failed
    }

    /** Một lệnh kèm dữ liệu đã đọc sẵn để đẩy. */
    private data class Outgoing(val doc: RemoteDoc, val pushed: PushedCommand)

    /**
     * Đọc các dòng dữ liệu mà lô lệnh trỏ tới. Lệnh không chép nội dung, nên thứ lên đám mây
     * luôn là bản mới nhất của dòng — kể cả dòng đã đánh dấu xoá (để máy khác biết mà xoá theo).
     */
    private suspend fun prepare(commands: List<SyncQueueEntity>): List<Outgoing> {
        fun idsOf(type: SyncEntityType) = commands.filter { it.entityType == type }.map { it.entityId }

        // id của dòng -> (các trường để ghi lên đám mây, updatedAt của dòng lúc đọc)
        val rows = HashMap<String, Pair<Map<String, Any?>, Long>>()
        syncDao.decksByIds(idsOf(SyncEntityType.DECK)).forEach { rows[it.id] = it.toRemoteFields() to it.updatedAt }
        syncDao.notesByIds(idsOf(SyncEntityType.NOTE)).forEach { rows[it.id] = it.toRemoteFields() to it.updatedAt }
        syncDao.flashcardsByIds(idsOf(SyncEntityType.FLASHCARD))
            .forEach { rows[it.id] = it.toRemoteFields() to it.updatedAt }

        // Lệnh trỏ tới dòng không còn tồn tại (đã bị xoá hẳn) thì không còn gì để đẩy: bỏ lệnh.
        val (alive, dangling) = commands.partition { it.entityId in rows }
        dangling.forEach { queueDao.deleteById(it.id) }

        return alive.map { command ->
            val (fields, updatedAt) = rows.getValue(command.entityId)
            Outgoing(RemoteDoc(command.entityType, command.entityId, fields), PushedCommand(command, updatedAt))
        }
    }

    private companion object {
        /** Số lệnh đẩy trong một lần gửi (Firestore cho tối đa 500 lệnh ghi mỗi batch). */
        const val PUSH_BATCH = 200

        /** Một lệnh hỏng quá số lần này thì bị bỏ qua. */
        const val MAX_RETRY = 5

        /** Số bản ghi trộn trong một transaction — nằm dưới giới hạn tham số của SQLite. */
        const val MERGE_CHUNK = 300
    }
}
