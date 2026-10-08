package com.ledinhthi.ontaptld.feature.settings

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ledinhthi.ontaptld.core.data.local.db.AppDatabase
import com.ledinhthi.ontaptld.feature.capture.domain.repository.CaptureImageRepository
import com.ledinhthi.ontaptld.feature.capture.domain.model.CropRect
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteImageStore
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.settings.data.LocalDataRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * "Xoá toàn bộ dữ liệu trên máy" có xoá sạch database và ảnh ghi chú không. Chạy trên database
 * Room trong bộ nhớ và một thư mục `files` giả, nên KHÔNG đụng tới dữ liệu thật của app đang cài.
 */
@RunWith(AndroidJUnit4::class)
class DeleteAllLocalDataTest {

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private lateinit var fakeFilesDir: File
    private lateinit var noteImage: File
    private var tempImagesCleared = false

    /** Bọc Context thật nhưng trả về thư mục `files` giả, để NoteImageStore xoá trong đó. */
    private val sandboxContext by lazy {
        object : android.content.ContextWrapper(targetContext) {
            override fun getFilesDir(): File = fakeFilesDir
        }
    }

    /** Màn chụp không liên quan tới test này: chỉ cần biết hàm dọn ảnh tạm có được gọi. */
    private val captureImages = object : CaptureImageRepository {
        override suspend fun newPhotoFile() = error("không dùng")
        override suspend fun importImage(uri: String) = error("không dùng")
        override suspend fun cropImage(sourcePath: String, crop: CropRect) = error("không dùng")
        override suspend fun keepNoteImage(tempPath: String, noteId: String) = error("không dùng")
        override suspend fun deleteNoteImage(path: String) = error("không dùng")
        override suspend fun clearTempImages() {
            tempImagesCleared = true
        }
    }

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(targetContext, AppDatabase::class.java).build()
        fakeFilesDir = File(targetContext.cacheDir, "delete-all-test").apply {
            deleteRecursively()
            mkdirs()
        }
        noteImage = File(File(fakeFilesDir, NoteImageStore.DIRECTORY).apply { mkdirs() }, "n1.jpg")
        noteImage.writeText("jpg")

        db.deckDao().upsert(DeckEntity(id = "d1", name = "IELTS", colorHex = "#4F46E5", createdAt = 1, updatedAt = 1))
        db.noteDao().upsert(
            NoteEntity(id = "n1", deckId = "d1", imagePath = noteImage.path, ocrText = "ghi chú", createdAt = 1, updatedAt = 1),
        )
        db.flashcardDao().upsert(
            FlashcardEntity(
                id = "c1", deckId = "d1", noteId = "n1", source = FlashcardSource.AI,
                question = "Q", answer = "A", dueDate = 1, createdAt = 1, updatedAt = 1,
            ),
        )
    }

    @After
    fun tearDown() {
        db.close()
        fakeFilesDir.deleteRecursively()
    }

    @Test
    fun xoaToanBo_databaseTrong_anhGhiChuMat_anhTamDuocDon() = runBlocking {
        val repository = LocalDataRepositoryImpl(
            database = db,
            noteImages = NoteImageStore(sandboxContext, Dispatchers.IO),
            captureImages = captureImages,
            io = Dispatchers.IO,
        )

        repository.deleteAll()

        // Những nơi đang nghe database (vd Home) phải nhận được danh sách rỗng.
        assertEquals(emptyList<DeckEntity>(), db.deckDao().observeAll().first())
        assertEquals(emptyList<FlashcardEntity>(), db.flashcardDao().observeByDeck("d1").first())
        assertNull(db.flashcardDao().getById("c1")) // xoá hẳn dòng, không chỉ đánh dấu
        assertNull(db.noteDao().getById("n1"))
        assertFalse(noteImage.exists())
        assertTrue(tempImagesCleared)
    }
}
