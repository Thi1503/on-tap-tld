package com.ledinhthi.ontaptld.feature.deck

import android.content.Context
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.sync.SyncEntityType
import com.ledinhthi.ontaptld.core.sync.SyncQueueRecorder
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckDao
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardDao
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteDao
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteImageStore
import com.ledinhthi.ontaptld.feature.deck.data.repository.DeckRepositoryImpl
import com.ledinhthi.ontaptld.feature.deck.data.repository.FlashcardRepositoryImpl
import com.ledinhthi.ontaptld.feature.deck.data.repository.NoteRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Xoá bộ thẻ / xoá thẻ / quét dọn lúc mở app có xoá đúng FILE ảnh ghi chú hay không. Database
 * được giả bằng mockk (phần SQL có test riêng chạy trên emulator: `NoteCleanupDaoTest`), còn file
 * là file thật trong một thư mục tạm mà JUnit tự xoá sau mỗi test.
 */
class NoteCleanupTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val now = 1_000_000_000L
    private val clock = mockk<Clock> { every { nowMillis() } returns now }
    private val deckDao = mockk<DeckDao>()
    private val cardDao = mockk<FlashcardDao>()
    private val noteDao = mockk<NoteDao>(relaxed = true)
    // Bộ ghi lệnh đồng bộ: ở đây chỉ cần nó "có mặt"; việc nó ghi gì có test riêng bên dưới.
    private val sync = mockk<SyncQueueRecorder>(relaxed = true)

    private lateinit var notesDirectory: File
    private lateinit var images: NoteImageStore

    @Before
    fun setUp() {
        val filesDir = tempFolder.newFolder("files")
        notesDirectory = File(filesDir, NoteImageStore.DIRECTORY).apply { mkdirs() }
        val context = mockk<Context>()
        every { context.filesDir } returns filesDir
        // Dispatchers.Unconfined: chạy việc đọc / ghi file ngay tại chỗ, test khỏi phải chờ luồng nền.
        images = NoteImageStore(context, Dispatchers.Unconfined)
    }

    /** Tạo một file ảnh giả; [ageMillis] = file đã nằm đó bao lâu tính tới "bây giờ" của test. */
    private fun image(name: String, ageMillis: Long = 3_600_000): File =
        File(notesDirectory, name).apply {
            writeText("jpg")
            setLastModified(now - ageMillis)
        }

    @Test
    fun `xoa bo the - xoa het anh ghi chu cua bo, anh cua bo khac con nguyen`() = runTest {
        val first = image("n1.jpg")
        val second = image("n2.jpg")
        val otherDeck = image("n3.jpg")
        coEvery { deckDao.softDeleteWithContents("d1", now) } returns listOf(first.path, second.path)

        DeckRepositoryImpl(deckDao, mockk(), clock, images, sync).delete("d1")

        assertFalse(first.exists())
        assertFalse(second.exists())
        assertTrue(otherDeck.exists())
    }

    @Test
    fun `xoa bo the ma database loi - khong dong toi anh`() = runTest {
        val kept = image("n1.jpg")
        coEvery { deckDao.softDeleteWithContents(any(), any()) } throws IllegalStateException("db")

        val result = runCatching { DeckRepositoryImpl(deckDao, mockk(), clock, images, sync).delete("d1") }

        assertTrue(result.isFailure)
        assertTrue(kept.exists())
    }

    @Test
    fun `xoa the - chi xoa anh khi ghi chu khong con the nao dung`() = runTest {
        val shared = image("n1.jpg")
        val repository = FlashcardRepositoryImpl(cardDao, mockk(), clock, images, sync)

        // Ghi chú còn thẻ khác dùng: database không trả về ảnh nào cần xoá.
        coEvery { cardDao.softDeleteAndReleaseNote("c1", now) } returns null
        repository.delete("c1")
        assertTrue(shared.exists())

        // Thẻ cuối cùng của ghi chú: database trả về đường dẫn ảnh.
        coEvery { cardDao.softDeleteAndReleaseNote("c2", now) } returns shared.path
        repository.delete("c2")
        assertFalse(shared.exists())
    }

    @Test
    fun `quet don luc mo app - xoa anh khong con ghi chu nao tro toi, giu anh con dung`() = runTest {
        val live = image("live.jpg")
        val orphan = image("orphan.jpg")
        coEvery { noteDao.liveImagePaths() } returns listOf(live.path)

        NoteRepositoryImpl(noteDao, mockk(), clock, images, sync).cleanUpOrphans()

        assertTrue(live.exists())
        assertFalse(orphan.exists())
        // Ghi chú mồ côi trong database cũng được dọn, chừa ghi chú mới tạo trong một phút gần đây.
        coVerify { noteDao.softDeleteOrphans(now, createdBefore = now - 60_000) }
    }

    @Test
    fun `quet don luc mo app - chua dung toi anh vua chep vao`() = runTest {
        // Ảnh mới 5 giây tuổi và chưa có ghi chú: có thể một lượt lưu thẻ AI đang chạy dở.
        val justSaved = image("new.jpg", ageMillis = 5_000)
        coEvery { noteDao.liveImagePaths() } returns emptyList()

        NoteRepositoryImpl(noteDao, mockk(), clock, images, sync).cleanUpOrphans()

        assertTrue(justSaved.exists())
    }

    @Test
    fun `quet don luc mo app - so theo ten file nen duong dan viet kieu khac van khop`() = runTest {
        val live = image("live.jpg")
        coEvery { noteDao.liveImagePaths() } returns listOf("/data/user/0/app/files/notes/live.jpg")

        NoteRepositoryImpl(noteDao, mockk(), clock, images, sync).cleanUpOrphans()

        assertTrue(live.exists())
    }

    @Test
    fun `quet don khi chua tung luu the AI - thu muc chua co, khong loi`() = runTest {
        notesDirectory.deleteRecursively()
        coEvery { noteDao.liveImagePaths() } returns emptyList()

        NoteRepositoryImpl(noteDao, mockk(), clock, images, sync).cleanUpOrphans()

        assertEquals(0, images.deleteUnreferenced(emptyList(), now))
    }
}
