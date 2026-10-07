package com.ledinhthi.ontaptld.core.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Dựng database ở schema cũ (đọc từ `app/schemas/.../<version>.json`), chạy Migration, rồi để
 * Room đối chiếu kết quả với schema mới. Thêm 1 test ở đây cho MỖI Migration mới.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate1To2_giuDuLieuCu_vaThemBangMoi() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO decks (id, userId, name, colorHex, createdAt, updatedAt, synced, isDeleted) " +
                    "VALUES ('d1', NULL, 'IELTS', '#4F46E5', 1, 1, 0, 0)",
            )
            execSQL(
                "INSERT INTO flashcards (id, deckId, noteId, source, question, answer, easeFactor, " +
                    "interval, repetitions, dueDate, createdAt, updatedAt, synced, isDeleted) " +
                    "VALUES ('c1', 'd1', NULL, 'MANUAL', 'Q', 'A', 2.5, 6, 2, 100, 1, 1, 0, 0)",
            )
            close()
        }

        // validateDroppedTables = true: Room kiểm tra schema sau migration khớp hẳn 2.json.
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT name FROM decks WHERE id = 'd1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("IELTS", c.getString(0))
        }
        db.query("SELECT interval, repetitions FROM flashcards WHERE id = 'c1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(6, c.getInt(0))
            assertEquals(2, c.getInt(1))
        }
        db.query("SELECT COUNT(*) FROM sync_queue").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM review_logs").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
