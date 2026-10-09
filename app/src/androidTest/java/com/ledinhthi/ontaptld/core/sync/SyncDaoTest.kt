package com.ledinhthi.ontaptld.core.sync

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ledinhthi.ontaptld.core.data.local.db.AppDatabase
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteEntity
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kiểm tra phần SQL của đồng bộ trên một database Room THẬT nằm trong bộ nhớ: ghi lệnh vào hàng
 * đợi, đánh dấu sau khi đẩy, trộn dữ liệu kéo về (Last-Write-Wins), nhận dữ liệu về một tài khoản.
 *
 * Chạy riêng lớp này mà không gỡ app: xem Mục 6.4 của docs/KE_HOACH_PHAT_TRIEN.md.
 */
@RunWith(AndroidJUnit4::class)
class SyncDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var sync: SyncDao
    private lateinit var queue: SyncQueueDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
        ).build()
        sync = db.syncDao()
        queue = db.syncQueueDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun commandOf(entityId: String) = queue.findByEntityId(entityId)
    private suspend fun cardRow(id: String) = sync.flashcardsByIds(listOf(id)).single()

    // ---------------------------------------------------------------------------------------
    // Ghi lệnh
    // ---------------------------------------------------------------------------------------

    @Test
    fun theVuaTao_lenhLaCreate_suaTiepTruocKhiDay_vanLaCreate_vaChiMotLenh() = runBlocking {
        db.flashcardDao().upsert(card("c1", createdAt = 100, updatedAt = 100))
        sync.enqueueFlashcards(listOf("c1"), now = 100)
        assertEquals(SyncAction.CREATE, commandOf("c1")?.action)

        // Sửa thẻ khi lệnh CREATE còn chờ: trên đám mây chưa hề có thẻ này, nên vẫn là "tạo".
        db.flashcardDao().upsert(card("c1", createdAt = 100, updatedAt = 250))
        sync.enqueueFlashcards(listOf("c1"), now = 250)

        assertEquals(SyncAction.CREATE, commandOf("c1")?.action)
        assertEquals(250L, commandOf("c1")?.createdAt)
        assertEquals(1, queue.getAll().size)
    }

    @Test
    fun theDaLenDamMay_suaThiLenhLaUpdate_xoaThiLenhLaDelete() = runBlocking {
        db.flashcardDao().upsert(card("c1", createdAt = 100, updatedAt = 300, synced = true))

        db.flashcardDao().upsert(card("c1", createdAt = 100, updatedAt = 400))
        sync.enqueueFlashcards(listOf("c1"), now = 400)
        assertEquals(SyncAction.UPDATE, commandOf("c1")?.action)

        db.flashcardDao().softDelete("c1", now = 500)
        sync.enqueueFlashcards(listOf("c1"), now = 500)
        assertEquals(SyncAction.DELETE, commandOf("c1")?.action)
        assertEquals(SyncEntityType.FLASHCARD, commandOf("c1")?.entityType)
        assertEquals(1, queue.getAll().size)
    }

    @Test
    fun ghiLenhMoi_datLaiSoLanHong() = runBlocking {
        db.flashcardDao().upsert(card("c1"))
        sync.enqueueFlashcards(listOf("c1"), now = 100)
        queue.markFailed("c1", "lỗi")
        assertEquals(1, commandOf("c1")?.retryCount)

        sync.enqueueFlashcards(listOf("c1"), now = 200)

        assertEquals(0, commandOf("c1")?.retryCount)
        assertNull(commandOf("c1")?.lastError)
    }

    @Test
    fun quet_chiThemLenhChoDongChuaDongBoMaChuaCoLenh() = runBlocking {
        db.deckDao().upsertAll(listOf(deck("d-moi"), deck("d-xong", synced = true)))
        db.noteDao().upsert(note("n1", "d-moi"))
        db.flashcardDao().upsertAll(listOf(card("c-co-lenh"), card("c-chua")))
        sync.enqueueFlashcards(listOf("c-co-lenh"), now = 50)

        sync.enqueueUnsynced(now = 900)

        assertEquals(setOf("d-moi", "n1", "c-co-lenh", "c-chua"), queue.getAll().map { it.entityId }.toSet())
        // Lệnh có sẵn không bị đụng tới.
        assertEquals(50L, commandOf("c-co-lenh")?.createdAt)
        assertEquals(SyncEntityType.DECK, commandOf("d-moi")?.entityType)
        assertEquals(SyncEntityType.NOTE, commandOf("n1")?.entityType)
    }

    @Test
    fun xoaBoThe_cacTheVaGhiChuBenTrongCungCoLenhDelete() = runBlocking {
        db.deckDao().upsert(deck("d1", synced = true))
        db.noteDao().upsert(note("n1", "d1", synced = true))
        db.flashcardDao().upsertAll(listOf(card("c1", synced = true), card("c2", synced = true)))

        db.deckDao().softDeleteWithContents("d1", now = 700)
        sync.enqueueDecks(listOf("d1"), now = 700)
        sync.enqueueUnsynced(now = 700)

        val commands = queue.getAll().associate { it.entityId to it.action }
        assertEquals(
            mapOf(
                "d1" to SyncAction.DELETE,
                "n1" to SyncAction.DELETE,
                "c1" to SyncAction.DELETE,
                "c2" to SyncAction.DELETE,
            ),
            commands,
        )
    }

    // ---------------------------------------------------------------------------------------
    // Đẩy lên
    // ---------------------------------------------------------------------------------------

    @Test
    fun layLoLenh_cuTruoc_boLenhHongQuaNguongVaLenhBiLoai() = runBlocking {
        db.flashcardDao().upsertAll(listOf(card("c1"), card("c2"), card("c3"), card("c4")))
        sync.enqueueFlashcards(listOf("c3"), now = 300)
        sync.enqueueFlashcards(listOf("c1"), now = 100)
        sync.enqueueFlashcards(listOf("c2"), now = 200)
        sync.enqueueFlashcards(listOf("c4"), now = 400)
        repeat(5) { queue.markFailed("c2", "hỏng") }

        val batch = queue.nextBatch(limit = 10, maxRetry = 5, excludedIds = listOf("c4"))

        assertEquals(listOf("c1", "c3"), batch.map { it.entityId })
        // Danh sách loại trừ rỗng vẫn chạy được.
        assertEquals(3, queue.nextBatch(limit = 10, maxRetry = 5, excludedIds = emptyList()).size)
    }

    @Test
    fun dayXong_dongThanhDaDongBo_lenhRoiHangDoi() = runBlocking {
        db.flashcardDao().upsert(card("c1", updatedAt = 100))
        sync.enqueueFlashcards(listOf("c1"), now = 100)
        val command = commandOf("c1")!!

        sync.markPushed(listOf(PushedCommand(command, rowUpdatedAt = 100)))

        assertTrue(cardRow("c1").synced)
        assertNull(commandOf("c1"))
    }

    @Test
    fun dongBiSuaTrongLucDangDay_vanLaChuaDongBo_lenhMoiDuocGiuLai() = runBlocking {
        db.flashcardDao().upsert(card("c1", updatedAt = 100))
        sync.enqueueFlashcards(listOf("c1"), now = 100)
        val pushedCommand = commandOf("c1")!! // lệnh và nội dung đã được đọc ra để gửi đi…

        // …thì người dùng sửa thẻ lần nữa.
        db.flashcardDao().upsert(card("c1", updatedAt = 180))
        sync.enqueueFlashcards(listOf("c1"), now = 180)

        sync.markPushed(listOf(PushedCommand(pushedCommand, rowUpdatedAt = 100)))

        assertFalse(cardRow("c1").synced)
        assertEquals(180L, commandOf("c1")?.createdAt)
    }

    // ---------------------------------------------------------------------------------------
    // Kéo về
    // ---------------------------------------------------------------------------------------

    @Test
    fun keoVe_banMoiHonThiDe_banCuHonThiBo_banChuaCoThiThem_banDaXoaThiKhongTao() = runBlocking {
        db.flashcardDao().upsertAll(
            listOf(
                card("cu-hon", updatedAt = 100, question = "bản trên máy"),
                card("moi-hon", updatedAt = 900, question = "bản trên máy"),
            ),
        )
        sync.enqueueFlashcards(listOf("cu-hon", "moi-hon"), now = 900)

        val written = sync.mergePulledFlashcards(
            listOf(
                card("cu-hon", updatedAt = 500, question = "bản đám mây", synced = true),
                card("moi-hon", updatedAt = 500, question = "bản đám mây", synced = true),
                card("chua-co", updatedAt = 500, question = "bản đám mây", synced = true),
                card("da-xoa", updatedAt = 500, synced = true, isDeleted = true),
            ),
        )

        assertEquals(2, written)
        // Thua: bị ghi đè, coi như đã đồng bộ, và lệnh đang chờ của nó bị bỏ.
        assertEquals("bản đám mây", cardRow("cu-hon").question)
        assertTrue(cardRow("cu-hon").synced)
        assertNull(commandOf("cu-hon"))
        // Thắng: giữ nguyên, lệnh vẫn chờ để lượt đẩy đưa nó lên.
        assertEquals("bản trên máy", cardRow("moi-hon").question)
        assertFalse(cardRow("moi-hon").synced)
        assertEquals("moi-hon", commandOf("moi-hon")?.entityId)
        assertEquals("bản đám mây", cardRow("chua-co").question)
        assertTrue(sync.flashcardsByIds(listOf("da-xoa")).isEmpty())
    }

    @Test
    fun keoVe_mayKhacXoaThe_mayNayXoaTheo() = runBlocking {
        db.flashcardDao().upsert(card("c1", updatedAt = 100, synced = true))

        sync.mergePulledFlashcards(listOf(card("c1", updatedAt = 300, synced = true, isDeleted = true)))

        assertTrue(cardRow("c1").isDeleted)
        assertTrue(cardRow("c1").synced)
    }

    @Test
    fun keoGhiChu_giuDuongDanAnhCuaMayNay_ghiChuMoiThiKhongCoAnh() = runBlocking {
        db.noteDao().upsert(note("n1", "d1", updatedAt = 100, synced = true))

        sync.mergePulledNotes(
            listOf(
                note("n1", "d1", updatedAt = 300, synced = true).copy(imagePath = "", ocrText = "sửa ở máy khác"),
                note("n2", "d1", updatedAt = 300, synced = true).copy(imagePath = ""),
            ),
        )

        val rows = sync.notesByIds(listOf("n1", "n2")).associateBy { it.id }
        assertEquals("/notes/n1.jpg", rows.getValue("n1").imagePath)
        assertEquals("sửa ở máy khác", rows.getValue("n1").ocrText)
        assertEquals("", rows.getValue("n2").imagePath)
    }

    @Test
    fun boTheBiMayKhacXoa_theVuaThemTrenMayNayCungBiXoaVaCoLenh() = runBlocking {
        db.deckDao().upsert(deck("d1", synced = true))
        db.flashcardDao().upsert(card("c-moi", updatedAt = 100))
        sync.mergePulledDecks(listOf(deck("d1", updatedAt = 400, synced = true, isDeleted = true)))

        sync.softDeleteContentsOfDeletedDecks(now = 450)

        assertTrue(cardRow("c-moi").isDeleted)
        assertEquals("c-moi", commandOf("c-moi")?.entityId)
    }

    // ---------------------------------------------------------------------------------------
    // Nhận dữ liệu về một tài khoản
    // ---------------------------------------------------------------------------------------

    @Test
    fun nhanDuLieuVeTaiKhoan_moiThuChuaDongBo_vaoHetHangDoi_boTheMangUid() = runBlocking {
        db.deckDao().upsert(deck("d1", synced = true))
        db.noteDao().upsert(note("n1", "d1", synced = true))
        db.flashcardDao().upsertAll(listOf(card("c1", synced = true), card("c2")))
        sync.enqueueFlashcards(listOf("c2"), now = 10)
        queue.markFailed("c2", "lỗi của tài khoản cũ")

        sync.claimAllFor("u-moi", now = 2_000)

        assertEquals(setOf("d1", "n1", "c1", "c2"), queue.getAll().map { it.entityId }.toSet())
        assertEquals(0, commandOf("c2")?.retryCount)
        assertEquals("u-moi", sync.decksByIds(listOf("d1")).single().userId)
        assertFalse(sync.decksByIds(listOf("d1")).single().synced)
        assertFalse(cardRow("c1").synced)
    }

    @Test
    fun ganUidChoBoTheTaoSauDangNhap_khongLamNoThanhChuaDongBo() = runBlocking {
        db.deckDao().upsert(deck("d1", synced = true)) // userId = null

        sync.assignDecksTo("u1")

        val row = sync.decksByIds(listOf("d1")).single()
        assertEquals("u1", row.userId)
        assertTrue(row.synced)
    }

    // ---------------------------------------------------------------------------------------

    private fun deck(id: String, updatedAt: Long = 1, synced: Boolean = false, isDeleted: Boolean = false) = DeckEntity(
        id = id, name = id, colorHex = "#4F46E5", createdAt = 1, updatedAt = updatedAt,
        synced = synced, isDeleted = isDeleted,
    )

    private fun note(id: String, deckId: String, updatedAt: Long = 1, synced: Boolean = false) = NoteEntity(
        id = id, deckId = deckId, imagePath = "/notes/$id.jpg", ocrText = "ghi chú $id",
        createdAt = 1, updatedAt = updatedAt, synced = synced,
    )

    private fun card(
        id: String,
        createdAt: Long = 1,
        updatedAt: Long = createdAt,
        question: String = "Q $id",
        synced: Boolean = false,
        isDeleted: Boolean = false,
    ) = FlashcardEntity(
        id = id, deckId = "d1", source = FlashcardSource.MANUAL, question = question, answer = "A $id",
        dueDate = 1, createdAt = createdAt, updatedAt = updatedAt, synced = synced, isDeleted = isDeleted,
    )
}
