package com.ledinhthi.ontaptld.feature.deck

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kiểm tra phần SQL của việc dọn ghi chú, trên một database Room THẬT nhưng chỉ nằm trong bộ nhớ
 * (`inMemoryDatabaseBuilder`): tạo mới cho mỗi test, không đụng tới database của app đang cài.
 *
 * Chạy riêng lớp này mà không gỡ app: xem Mục 6.4 của docs/KE_HOACH_PHAT_TRIEN.md.
 */
@RunWith(AndroidJUnit4::class)
class NoteCleanupDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
        ).build()
        db.deckDao().upsertAll(listOf(deck("d1"), deck("d2")))
        // n1 sinh ra hai thẻ c1, c2; n2 chỉ có c3; n3 thuộc bộ thẻ khác.
        db.noteDao().upsertAll(listOf(note("n1", "d1"), note("n2", "d1"), note("n3", "d2")))
        db.flashcardDao().upsertAll(
            listOf(
                card("c1", "d1", "n1"),
                card("c2", "d1", "n1"),
                card("c3", "d1", "n2"),
                card("c4", "d1", noteId = null), // thẻ thủ công
                card("c5", "d2", "n3"),
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun xoaBoThe_xoaLuonGhiChuCuaBo_traVeDuongDanAnh() = runBlocking {
        val imagePaths = db.deckDao().softDeleteWithContents("d1", now = 500)

        assertEquals(setOf("/notes/n1.jpg", "/notes/n2.jpg"), imagePaths.toSet())
        assertNull(db.noteDao().getById("n1"))
        assertNull(db.noteDao().getById("n2"))
        // Bộ thẻ khác không bị ảnh hưởng.
        assertNotNull(db.noteDao().getById("n3"))
        assertEquals(listOf("/notes/n3.jpg"), db.noteDao().liveImagePaths())
    }

    @Test
    fun xoaThe_ghiChuConTheKhacDung_thiGiuGhiChu() = runBlocking {
        val imagePath = db.flashcardDao().softDeleteAndReleaseNote("c1", now = 500)

        assertNull(imagePath)
        assertNotNull(db.noteDao().getById("n1"))
    }

    @Test
    fun xoaTheCuoiCungCuaGhiChu_xoaLuonGhiChu_traVeDuongDanAnh() = runBlocking {
        db.flashcardDao().softDeleteAndReleaseNote("c1", now = 500)
        val imagePath = db.flashcardDao().softDeleteAndReleaseNote("c2", now = 600)

        assertEquals("/notes/n1.jpg", imagePath)
        assertNull(db.noteDao().getById("n1"))
        // Ghi chú khác trong cùng bộ thẻ còn nguyên.
        assertNotNull(db.noteDao().getById("n2"))
    }

    @Test
    fun xoaTheThuCong_khongDungToiGhiChuNao() = runBlocking {
        val imagePath = db.flashcardDao().softDeleteAndReleaseNote("c4", now = 500)

        assertNull(imagePath)
        assertEquals(3, db.noteDao().liveImagePaths().size)
    }

    @Test
    fun quetGhiChuMoCoi_chiXoaGhiChuHetTheVaDaCu() = runBlocking {
        // Xoá thẻ theo kiểu của bản app cũ: chỉ đánh dấu thẻ, bỏ lại ghi chú n2.
        db.flashcardDao().softDelete("c3", now = 500)
        // Ghi chú vừa tạo (createdAt = 950), thẻ chưa kịp ghi — như đang lưu thẻ AI dở chừng.
        db.noteDao().upsert(note("moi", "d1", createdAt = 950))

        val deleted = db.noteDao().softDeleteOrphans(now = 1_000, createdBefore = 900)

        assertEquals(1, deleted)
        assertNull(db.noteDao().getById("n2"))
        assertNotNull(db.noteDao().getById("n1"))
        assertNotNull(db.noteDao().getById("n3"))
        assertNotNull(db.noteDao().getById("moi"))
    }

    private fun deck(id: String) =
        DeckEntity(id = id, name = id, colorHex = "#4F46E5", createdAt = 1, updatedAt = 1)

    private fun note(id: String, deckId: String, createdAt: Long = 1) = NoteEntity(
        id = id,
        deckId = deckId,
        imagePath = "/notes/$id.jpg",
        ocrText = "ghi chú $id",
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun card(id: String, deckId: String, noteId: String?) = FlashcardEntity(
        id = id,
        deckId = deckId,
        noteId = noteId,
        source = if (noteId == null) FlashcardSource.MANUAL else FlashcardSource.AI,
        question = "Q $id",
        answer = "A $id",
        dueDate = 1,
        createdAt = 1,
        updatedAt = 1,
    )
}
