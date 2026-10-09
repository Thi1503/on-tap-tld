package com.ledinhthi.ontaptld.feature.sync

import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.SyncErrorKind
import com.ledinhthi.ontaptld.core.sync.PushedCommand
import com.ledinhthi.ontaptld.core.sync.SyncAction
import com.ledinhthi.ontaptld.core.sync.SyncDao
import com.ledinhthi.ontaptld.core.sync.SyncEntityType
import com.ledinhthi.ontaptld.core.sync.SyncQueueDao
import com.ledinhthi.ontaptld.core.sync.SyncQueueEntity
import com.ledinhthi.ontaptld.feature.auth.domain.AuthRepository
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardEntity
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.sync.data.SyncRepositoryImpl
import com.ledinhthi.ontaptld.feature.sync.data.remote.RemoteChanges
import com.ledinhthi.ontaptld.feature.sync.data.remote.RemoteDoc
import com.ledinhthi.ontaptld.feature.sync.data.remote.SyncRemoteDataSource
import com.ledinhthi.ontaptld.feature.sync.data.remote.toRemoteFields
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bộ máy đồng bộ, với database và đám mây đều là bản giả (mockk): kiểm tra nó gọi ĐÚNG việc,
 * ĐÚNG thứ tự và xử lý lỗi thế nào. Phần SQL chạy thật có test riêng trên emulator: `SyncDaoTest`.
 */
class SyncRepositoryImplTest {

    private val now = 50_000L
    private val uid = "u1"

    private val auth = mockk<AuthRepository> { every { currentUserId } returns uid }
    private val syncDao = mockk<SyncDao>(relaxed = true)
    private val queueDao = mockk<SyncQueueDao>(relaxed = true)
    private val remote = mockk<SyncRemoteDataSource>()
    private val clock = mockk<Clock> { every { nowMillis() } returns now }

    // File cài đặt giả: giờ đồng bộ gần nhất nằm trong `lastSyncAt`, còn lại trả giá trị cố định.
    private val lastSyncAt = MutableStateFlow(0L)
    private val prefs = mockk<AppPreferences>(relaxed = true) {
        every { lastSyncAtMillis } returns lastSyncAt
        coEvery { setLastSyncAt(any()) } coAnswers { lastSyncAt.value = firstArg() }
        coEvery { getSyncOwnerUid() } returns uid // mặc định: máy đã đồng bộ với tài khoản này rồi
        coEvery { getSyncCursor(any()) } returns 0L
    }

    private val repository = SyncRepositoryImpl(auth, syncDao, queueDao, remote, prefs, clock, Dispatchers.Unconfined)

    private val card = FlashcardEntity(
        id = "c1", deckId = "d1", source = FlashcardSource.MANUAL, question = "Q", answer = "A",
        dueDate = 1_000, createdAt = 1_000, updatedAt = 2_000,
    )

    private fun command(entityId: String, type: SyncEntityType = SyncEntityType.FLASHCARD) = SyncQueueEntity(
        id = entityId, entityType = type, entityId = entityId, action = SyncAction.UPDATE, createdAt = 3_000,
    )

    /** Đám mây không có gì mới, hàng đợi trống — từng test sửa lại phần mình cần. */
    private fun nothingToDo() {
        coEvery { remote.fetchChangedSince(any(), any(), any()) } answers { RemoteChanges(emptyList(), thirdArg()) }
        coEvery { remote.upload(any(), any()) } returns Unit
        coEvery { queueDao.nextBatch(any(), any(), any()) } returns emptyList()
    }

    private suspend fun syncError(): AppException.SyncException =
        runCatching { repository.sync() }.exceptionOrNull() as AppException.SyncException

    @Test
    fun `chua dang nhap - bao loi, khong dung toi dam may`() = runTest {
        every { auth.currentUserId } returns null

        assertEquals(SyncErrorKind.NOT_SIGNED_IN, syncError().kind)

        coVerify(exactly = 0) { remote.fetchChangedSince(any(), any(), any()) }
        coVerify(exactly = 0) { syncDao.claimAllFor(any(), any()) }
    }

    @Test
    fun `lan dau dong bo voi tai khoan - xep het du lieu tren may vao hang doi, keo lai tu dau`() = runTest {
        nothingToDo()
        coEvery { prefs.getSyncOwnerUid() } returns null

        repository.sync()

        coVerifyOrder {
            syncDao.claimAllFor(uid, now)
            prefs.startSyncFor(uid)
            remote.fetchChangedSince(uid, SyncEntityType.DECK, any())
        }
    }

    @Test
    fun `doi sang tai khoan khac - du lieu tren may di theo tai khoan moi`() = runTest {
        nothingToDo()
        coEvery { prefs.getSyncOwnerUid() } returns "nguoi-cu"

        repository.sync()

        coVerify { syncDao.claimAllFor(uid, now) }
        coVerify { prefs.startSyncFor(uid) }
    }

    @Test
    fun `van tai khoan cu - khong xep lai tat ca, chi quet bo sung lenh con thieu`() = runTest {
        nothingToDo()

        repository.sync()

        coVerify(exactly = 0) { syncDao.claimAllFor(any(), any()) }
        coVerify(exactly = 0) { prefs.startSyncFor(any()) }
        coVerify { syncDao.assignDecksTo(uid) }
        coVerify { syncDao.enqueueUnsynced(now) }
    }

    @Test
    fun `keo ve - bo the truoc roi ghi chu roi the, tron xong moi doi moc`() = runTest {
        nothingToDo()
        val deck = DeckEntity(id = "d1", name = "Sinh học", colorHex = "#0D9488", createdAt = 1, updatedAt = 9)
        coEvery { prefs.getSyncCursor("DECK") } returns 40L
        coEvery { remote.fetchChangedSince(uid, SyncEntityType.DECK, 40L) } returns
            RemoteChanges(listOf(RemoteDoc(SyncEntityType.DECK, "d1", deck.toRemoteFields())), cursor = 99L)

        repository.sync()

        coVerifyOrder {
            remote.fetchChangedSince(uid, SyncEntityType.DECK, 40L)
            // Bản ghi kéo về mang uid của người đăng nhập và được coi là đã đồng bộ.
            syncDao.mergePulledDecks(listOf(deck.copy(userId = uid, synced = true)))
            prefs.setSyncCursor("DECK", 99L)
            remote.fetchChangedSince(uid, SyncEntityType.NOTE, 0L)
            remote.fetchChangedSince(uid, SyncEntityType.FLASHCARD, 0L)
            // Kéo xong mới đẩy.
            queueDao.nextBatch(any(), any(), any())
        }
    }

    @Test
    fun `ban ghi hong tren dam may - bo qua, luot dong bo van xong`() = runTest {
        nothingToDo()
        coEvery { remote.fetchChangedSince(uid, SyncEntityType.FLASHCARD, any()) } returns
            RemoteChanges(listOf(RemoteDoc(SyncEntityType.FLASHCARD, "hong", mapOf("question" to 12))), cursor = 7L)

        repository.sync()

        coVerify { syncDao.mergePulledFlashcards(emptyList()) }
        coVerify { prefs.setSyncCursor("FLASHCARD", 7L) }
    }

    @Test
    fun `day len - ghi ban moi nhat cua dong len dam may, danh dau da dong bo, ghi gio dong bo`() = runTest {
        nothingToDo()
        val cmd = command("c1")
        coEvery { queueDao.nextBatch(any(), any(), any()) } returnsMany listOf(listOf(cmd), emptyList())
        coEvery { syncDao.flashcardsByIds(listOf("c1")) } returns listOf(card)

        repository.sync()

        coVerifyOrder {
            remote.upload(uid, listOf(RemoteDoc(SyncEntityType.FLASHCARD, "c1", card.toRemoteFields())))
            // Kèm `updatedAt` lúc đọc: dòng bị sửa trong lúc đang đẩy thì sẽ không bị đánh dấu nhầm.
            syncDao.markPushed(listOf(PushedCommand(cmd, rowUpdatedAt = 2_000)))
            prefs.setLastSyncAt(now)
        }
        val status = repository.status.first()
        assertEquals(now, status.lastSyncedAtMillis)
        assertFalse(status.lastFailed)
        assertFalse(status.isSyncing)
    }

    @Test
    fun `lenh tro toi dong da bi xoa han - bo lenh, khong gui gi len`() = runTest {
        nothingToDo()
        coEvery { queueDao.nextBatch(any(), any(), any()) } returnsMany listOf(listOf(command("c-mat")), emptyList())
        coEvery { syncDao.flashcardsByIds(any()) } returns emptyList()

        repository.sync()

        coVerify { queueDao.deleteById("c-mat") }
        coVerify(exactly = 0) { remote.upload(any(), any()) }
    }

    @Test
    fun `mat mang luc day - dung luot, hang doi con nguyen, trang thai la that bai`() = runTest {
        nothingToDo()
        coEvery { queueDao.nextBatch(any(), any(), any()) } returns listOf(command("c1"))
        coEvery { syncDao.flashcardsByIds(any()) } returns listOf(card)
        coEvery { remote.upload(any(), any()) } throws AppException.SyncException(SyncErrorKind.NETWORK)

        assertEquals(SyncErrorKind.NETWORK, syncError().kind)

        // Mất mạng không phải lỗi của lệnh: không tính là một lần hỏng, không rút khỏi hàng đợi.
        coVerify(exactly = 0) { queueDao.markFailed(any(), any()) }
        coVerify(exactly = 0) { syncDao.markPushed(any()) }
        coVerify(exactly = 0) { prefs.setLastSyncAt(any()) }
        val status = repository.status.first()
        assertTrue(status.lastFailed)
        assertNull(status.lastSyncedAtMillis)
    }

    @Test
    fun `mot lo bi loi la - day lai tung lenh, lenh lanh van di, lenh hong duoc ghi nhan`() = runTest {
        nothingToDo()
        val good = command("c1")
        val bad = command("c2")
        val badCard = card.copy(id = "c2")
        coEvery { queueDao.nextBatch(any(), any(), any()) } returnsMany listOf(listOf(good, bad), emptyList())
        coEvery { syncDao.flashcardsByIds(any()) } returns listOf(card, badCard)
        val unknown = AppException.SyncException(SyncErrorKind.UNKNOWN, IllegalStateException("bản ghi quá lớn"))
        // Cả lô hỏng; đẩy riêng thì c1 đi được, c2 vẫn hỏng.
        coEvery { remote.upload(uid, match { it.size == 2 }) } throws unknown
        coEvery { remote.upload(uid, match { it.size == 1 && it[0].id == "c1" }) } returns Unit
        coEvery { remote.upload(uid, match { it.size == 1 && it[0].id == "c2" }) } throws unknown

        repository.sync() // không ném lỗi: một lệnh hỏng không làm hỏng cả lượt

        coVerify { syncDao.markPushed(listOf(PushedCommand(good, rowUpdatedAt = 2_000))) }
        coVerify { queueDao.markFailed("c2", "bản ghi quá lớn") }
        coVerify(exactly = 0) { syncDao.markPushed(match { list -> list.any { it.command.id == "c2" } }) }
    }

    @Test
    fun `dam may tu choi quyen - dung ngay, khong thu tung lenh`() = runTest {
        nothingToDo()
        coEvery { remote.fetchChangedSince(any(), any(), any()) } throws
            AppException.SyncException(SyncErrorKind.PERMISSION_DENIED)

        assertEquals(SyncErrorKind.PERMISSION_DENIED, syncError().kind)

        coVerify(exactly = 0) { remote.upload(any(), any()) }
        assertTrue(repository.status.first().lastFailed)
    }

    @Test
    fun `loi khong luong truoc - doi thanh loi dong bo de man hinh bao duoc`() = runTest {
        nothingToDo()
        coEvery { syncDao.enqueueUnsynced(any()) } throws IllegalStateException("database hỏng")

        assertEquals(SyncErrorKind.UNKNOWN, syncError().kind)
    }

    @Test
    fun `quen tai khoan - xoa moc dong bo`() = runTest {
        repository.forgetAccount()

        coVerify { prefs.clearSyncState() }
    }
}
