package com.ledinhthi.ontaptld.feature.sync

import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.sync.SyncDao
import com.ledinhthi.ontaptld.core.sync.SyncEntityType
import com.ledinhthi.ontaptld.core.sync.SyncQueueRecorder
import com.ledinhthi.ontaptld.core.sync.SyncScheduler
import com.ledinhthi.ontaptld.core.sync.collectionName
import com.ledinhthi.ontaptld.core.sync.remoteWins
import com.ledinhthi.ontaptld.feature.auth.domain.AuthRepository
import com.ledinhthi.ontaptld.feature.auth.domain.model.AuthUser
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteEntity
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.settings.domain.DeleteAllLocalDataUseCase
import com.ledinhthi.ontaptld.feature.settings.domain.LocalDataRepository
import com.ledinhthi.ontaptld.feature.sync.data.remote.toDeckEntity
import com.ledinhthi.ontaptld.feature.sync.data.remote.toFlashcardEntity
import com.ledinhthi.ontaptld.feature.sync.data.remote.toNoteEntity
import com.ledinhthi.ontaptld.feature.sync.data.remote.toRemoteFields
import com.ledinhthi.ontaptld.feature.sync.domain.KeepSyncedUseCase
import com.ledinhthi.ontaptld.feature.sync.domain.SyncAge
import com.ledinhthi.ontaptld.feature.sync.domain.SyncRepository
import com.ledinhthi.ontaptld.feature.sync.domain.syncAgeOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Các luật nhỏ của phần đồng bộ, mỗi luật là một hàm thuần hoặc một lớp mỏng: luật ai thắng khi
 * kéo về, đổi dữ liệu qua lại với Firestore, ghi lệnh vào hàng đợi, chữ "N phút trước", và mấy
 * use case đi kèm. Bộ máy đồng bộ có test riêng: `SyncRepositoryImplTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncRulesTest {

    // ---- Last-Write-Wins ----

    @Test
    fun `may chua co ban ghi - tao moi, tru khi no da bi xoa o may khac`() {
        assertTrue(remoteWins(localUpdatedAt = null, remoteUpdatedAt = 100, remoteDeleted = false))
        assertFalse(remoteWins(localUpdatedAt = null, remoteUpdatedAt = 100, remoteDeleted = true))
    }

    @Test
    fun `hai noi cung co - ban sua sau thi thang, bang nhau thi giu ban tren may`() {
        assertTrue(remoteWins(localUpdatedAt = 100, remoteUpdatedAt = 200, remoteDeleted = false))
        assertFalse(remoteWins(localUpdatedAt = 200, remoteUpdatedAt = 100, remoteDeleted = false))
        assertFalse(remoteWins(localUpdatedAt = 100, remoteUpdatedAt = 100, remoteDeleted = false))
    }

    @Test
    fun `may khac xoa SAU khi may nay sua - viec xoa thang`() {
        assertTrue(remoteWins(localUpdatedAt = 100, remoteUpdatedAt = 200, remoteDeleted = true))
    }

    // ---- Đổi dữ liệu qua lại với Firestore ----

    private val card = FlashcardEntity(
        id = "c1",
        deckId = "d1",
        noteId = "n1",
        source = FlashcardSource.AI,
        question = "ATP là gì?",
        answer = "Đồng tiền năng lượng của tế bào",
        sourceBoxLeft = 0.1f,
        sourceBoxTop = 0.2f,
        sourceBoxRight = 0.9f,
        sourceBoxBottom = 0.3f,
        sourceLine = 4,
        easeFactor = 2.36,
        interval = 6,
        repetitions = 2,
        dueDate = 9_000,
        lastReviewedAt = 8_000,
        createdAt = 1_000,
        updatedAt = 8_000,
        synced = false,
        isDeleted = false,
    )

    /** Firestore trả mọi số nguyên dưới dạng Long và số thực dưới dạng Double — giả lại đúng thế. */
    private fun Map<String, Any?>.asFirestoreReturnsIt(): Map<String, Any?> = mapValues { (_, value) ->
        when (value) {
            is Int -> value.toLong()
            is Float -> value.toDouble()
            else -> value
        }
    }

    @Test
    fun `the di len dam may roi ve lai - con nguyen ca tien do on, va duoc danh dau da dong bo`() {
        val back = card.toRemoteFields().asFirestoreReturnsIt().toFlashcardEntity("c1")

        assertEquals(card.copy(synced = true), back)
    }

    @Test
    fun `the thu cong khong co vung nguon - cac truong trong van la null`() {
        val manual = card.copy(
            noteId = null, source = FlashcardSource.MANUAL, sourceLine = null, lastReviewedAt = null,
            sourceBoxLeft = null, sourceBoxTop = null, sourceBoxRight = null, sourceBoxBottom = null,
        )

        val back = manual.toRemoteFields().asFirestoreReturnsIt().toFlashcardEntity("c1")

        assertEquals(manual.copy(synced = true), back)
    }

    @Test
    fun `bo the ve may - mang uid cua nguoi dang dang nhap`() {
        val deck = DeckEntity(id = "d1", name = "Sinh học", colorHex = "#0D9488", createdAt = 1, updatedAt = 2, isDeleted = true)

        val back = deck.toRemoteFields().toDeckEntity("d1", uid = "u1")

        assertEquals(deck.copy(userId = "u1", synced = true), back)
    }

    @Test
    fun `ghi chu chi len dam may phan chu - duong dan anh o lai tren may`() {
        val note = NoteEntity(
            id = "n1", deckId = "d1", imagePath = "/data/files/notes/n1.jpg", ocrText = "ATP…",
            createdAt = 1, updatedAt = 2,
        )

        val fields = note.toRemoteFields()
        val back = fields.toNoteEntity("n1")

        assertFalse("imagePath" in fields)
        assertEquals(note.copy(imagePath = "", synced = true), back)
    }

    @Test
    fun `ban ghi hong tren dam may - bo qua thay vi vang loi`() {
        assertNull(mapOf<String, Any?>("name" to "Thiếu các trường còn lại").toDeckEntity("d1", "u1"))
        assertNull(card.toRemoteFields().minus("question").toFlashcardEntity("c1"))
        assertNull(card.toRemoteFields().plus("source" to "HOLOGRAM").toFlashcardEntity("c1"))
        assertNull(mapOf<String, Any?>("deckId" to 42, "updatedAt" to "hôm qua").toNoteEntity("n1"))
    }

    @Test
    fun `ten collection dung cau truc da chot`() {
        assertEquals(
            listOf("decks", "notes", "flashcards"),
            SyncEntityType.entries.map { it.collectionName },
        )
    }

    // ---- Ghi lệnh vào hàng đợi ----

    private val clock = mockk<Clock> { every { nowMillis() } returns 777L }
    private val syncDao = mockk<SyncDao>(relaxed = true)
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private val recorder = SyncQueueRecorder(syncDao, clock, scheduler)

    @Test
    fun `ghi mot thay doi - xep lenh dung bang va xin mot luot dong bo`() = runTest {
        recorder.recordChanged(SyncEntityType.FLASHCARD, listOf("c1", "c2"))

        coVerify { syncDao.enqueueFlashcards(listOf("c1", "c2"), 777L) }
        coVerify(exactly = 0) { syncDao.enqueueDecks(any(), any()) }
        verify(exactly = 1) { scheduler.requestSync() }
    }

    @Test
    fun `khong co dong nao doi - khong ghi lenh, khong xin dong bo`() = runTest {
        recorder.recordChanged(SyncEntityType.DECK, emptyList())

        coVerify(exactly = 0) { syncDao.enqueueDecks(any(), any()) }
        verify(exactly = 0) { scheduler.requestSync() }
    }

    @Test
    fun `danh sach dai - cat thanh khuc de khong vuot gioi han tham so cua SQLite`() = runTest {
        val ids = List(1_200) { "c$it" }

        recorder.recordChanged(SyncEntityType.FLASHCARD, ids)

        coVerify(exactly = 3) { syncDao.enqueueFlashcards(any(), 777L) }
        verify(exactly = 1) { scheduler.requestSync() }
    }

    @Test
    fun `thay doi gian tiep - quet cac dong chua co lenh`() = runTest {
        recorder.recordCascades()

        coVerify { syncDao.enqueueUnsynced(777L) }
        verify(exactly = 1) { scheduler.requestSync() }
    }

    // ---- "N phút trước" ----

    @Test
    fun `tuoi cua lan dong bo - lam tron ve don vi de doc`() {
        val minute = 60_000L
        assertEquals(SyncAge.JustNow, syncAgeOf(nowMillis = 59_000, syncedAtMillis = 0))
        assertEquals(SyncAge.Minutes(1), syncAgeOf(minute, 0))
        assertEquals(SyncAge.Minutes(59), syncAgeOf(59 * minute + 59_000, 0))
        assertEquals(SyncAge.Hours(1), syncAgeOf(60 * minute, 0))
        assertEquals(SyncAge.Hours(23), syncAgeOf(24 * 60 * minute - 1, 0))
        assertEquals(SyncAge.Days(2), syncAgeOf(2 * 24 * 60 * minute, 0))
        // Đồng hồ máy bị chỉnh lùi: không hiện số âm.
        assertEquals(SyncAge.JustNow, syncAgeOf(nowMillis = 0, syncedAtMillis = 5_000))
    }

    // ---- Use case ----

    @Test
    fun `co nguoi dang nhap thi hen dong bo - dang xuat thi huy - Firebase bao lai cung nguoi thi bo qua`() =
        runTest(UnconfinedTestDispatcher()) {
            val userFlow = MutableStateFlow<AuthUser?>(null)
            val auth = mockk<AuthRepository> { every { currentUser } returns userFlow }
            // Use case này không bao giờ tự kết thúc -> chạy nó trong một coroutine riêng rồi huỷ.
            val job = launch { KeepSyncedUseCase(auth, scheduler).invoke() }
            verify(exactly = 1) { scheduler.cancel() } // mở app khi chưa đăng nhập

            userFlow.value = AuthUser("u1", "Minh Anh", "a@example.com")
            userFlow.value = AuthUser("u1", "Minh Anh (đổi tên)", "a@example.com") // vẫn là người đó
            verify(exactly = 1) { scheduler.requestSync() }

            userFlow.value = null
            verify(exactly = 2) { scheduler.cancel() }
            job.cancel()
        }

    @Test
    fun `xoa toan bo du lieu khi da dang nhap - dang xuat, quen moc dong bo, roi moi xoa`() = runTest {
        val local = mockk<LocalDataRepository>(relaxed = true)
        val sync = mockk<SyncRepository>(relaxed = true)
        val auth = mockk<AuthRepository>(relaxed = true) { every { currentUserId } returns "u1" }

        DeleteAllLocalDataUseCase(local, auth, sync).invoke()

        coVerifyOrder {
            auth.signOut()
            sync.forgetAccount()
            local.deleteAll()
        }
    }

    @Test
    fun `xoa toan bo du lieu khi chua dang nhap - khong dung toi tai khoan`() = runTest {
        val local = mockk<LocalDataRepository>(relaxed = true)
        val sync = mockk<SyncRepository>(relaxed = true)
        val auth = mockk<AuthRepository>(relaxed = true) { every { currentUserId } returns null }
        coEvery { sync.forgetAccount() } returns Unit

        DeleteAllLocalDataUseCase(local, auth, sync).invoke()

        coVerify(exactly = 0) { auth.signOut() }
        coVerify(exactly = 1) { local.deleteAll() }
    }
}
