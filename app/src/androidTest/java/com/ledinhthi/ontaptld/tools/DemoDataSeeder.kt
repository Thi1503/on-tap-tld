package com.ledinhthi.ontaptld.tools

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ledinhthi.ontaptld.core.data.local.db.ALL_MIGRATIONS
import com.ledinhthi.ontaptld.core.data.local.db.AppDatabase
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardEntity
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * CÔNG CỤ, không phải test: nạp dữ liệu mẫu (giống bản thiết kế) vào database của bản debug
 * đang cài trên máy/emulator — để chụp màn hình, demo, hoặc so giao diện với design.
 *
 * Bị bỏ qua trong các lần chạy test thông thường; chỉ chạy khi truyền `-e seedDemo true`:
 *
 * ```
 * ./gradlew :app:installDebug :app:installDebugAndroidTest
 * adb shell am instrument -w -e seedDemo true \
 *   -e class com.ledinhthi.ontaptld.tools.DemoDataSeeder \
 *   com.ledinhthi.ontaptld.test/androidx.test.runner.AndroidJUnitRunner
 * ```
 *
 * An toàn với dữ liệu thật: nếu database đã có bộ thẻ nào thì dừng, không ghi thêm/ghi đè.
 */
@RunWith(AndroidJUnit4::class)
class DemoDataSeeder {

    private data class DemoCard(val question: String, val answer: String, val source: FlashcardSource)

    private data class DemoDeck(
        val name: String,
        val colorHex: String,
        val cardCount: Int,
        val dueCount: Int,
        val featured: List<DemoCard> = emptyList(),
    )

    // Thứ tự khai báo = thứ tự hiển thị trên Home (bộ đầu có updatedAt mới nhất).
    private val demoDecks = listOf(
        DemoDeck(
            name = "IELTS Vocabulary", colorHex = "#4F46E5", cardCount = 128, dueCount = 12,
            featured = listOf(
                DemoCard("ubiquitous (adj)", "Có mặt ở khắp nơi; phổ biến đến mức đâu cũng thấy.", FlashcardSource.AI),
                DemoCard(
                    "Phân biệt affect và effect?",
                    "Affect là động từ (gây ảnh hưởng); effect là danh từ (kết quả, tác động).",
                    FlashcardSource.MANUAL,
                ),
                DemoCard("mitigate (v)", "Làm giảm nhẹ mức độ nghiêm trọng của một vấn đề.", FlashcardSource.AI),
                DemoCard("a double-edged sword", "Con dao hai lưỡi — vừa lợi vừa hại.", FlashcardSource.MANUAL),
            ),
        ),
        DemoDeck(
            name = "Sinh học 12 – Di truyền", colorHex = "#0D9488", cardCount = 74, dueCount = 8,
            featured = listOf(
                DemoCard("Nhân đôi ADN diễn ra ở pha nào của chu kì tế bào?", "Pha S của kì trung gian.", FlashcardSource.AI),
                DemoCard(
                    "Hai nguyên tắc của quá trình nhân đôi ADN là gì?",
                    "Nguyên tắc bổ sung (A–T, G–X) và nguyên tắc bán bảo toàn.",
                    FlashcardSource.AI,
                ),
                DemoCard("ADN pôlimeraza tổng hợp mạch mới theo chiều nào?", "Chiều 5'→3'.", FlashcardSource.AI),
                DemoCard("Đoạn Okazaki là gì?", "Các đoạn ADN ngắn được tổng hợp gián đoạn trên mạch chậm.", FlashcardSource.AI),
            ),
        ),
        DemoDeck(name = "Lịch sử Việt Nam", colorHex = "#DB2777", cardCount = 62, dueCount = 4),
        DemoDeck(name = "Kotlin Coroutines", colorHex = "#0284C7", cardCount = 52, dueCount = 0),
    )

    @Test
    fun seed() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Bỏ qua: chỉ chạy khi truyền -e seedDemo true", args.getString("seedDemo") == "true")

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "ontaptld.db")
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        try {
            check(db.deckDao().observeAll().first().isEmpty()) {
                "Database đã có bộ thẻ — DemoDataSeeder không ghi đè dữ liệu đang có."
            }
            val now = System.currentTimeMillis()
            demoDecks.forEachIndexed { order, demo ->
                val deckId = UUID.randomUUID().toString()
                val stamp = now - order * HOUR
                db.deckDao().upsert(
                    DeckEntity(
                        id = deckId,
                        name = demo.name,
                        colorHex = demo.colorHex,
                        createdAt = stamp - 30 * DAY,
                        updatedAt = stamp,
                    ),
                )
                db.flashcardDao().upsertAll(List(demo.cardCount) { i -> card(demo, deckId, i, stamp, now) })
            }
        } finally {
            db.close()
        }
    }

    private fun card(demo: DemoDeck, deckId: String, index: Int, stamp: Long, now: Long): FlashcardEntity {
        val featured = demo.featured.getOrNull(index)
        val isDue = index < demo.dueCount
        // createdAt giảm dần theo index -> thẻ mẫu có nội dung thật đứng đầu danh sách.
        val created = stamp - index * 1000L
        return FlashcardEntity(
            id = UUID.randomUUID().toString(),
            deckId = deckId,
            source = featured?.source ?: if (index % 3 == 0) FlashcardSource.MANUAL else FlashcardSource.AI,
            question = featured?.question ?: "${demo.name} — câu hỏi ${index + 1}",
            answer = featured?.answer ?: "Câu trả lời mẫu ${index + 1}",
            interval = if (isDue) 0 else 6,
            repetitions = if (isDue) 0 else 2,
            dueDate = if (isDue) now - HOUR else now + (1 + index % 15) * DAY,
            lastReviewedAt = if (isDue) null else now - DAY,
            createdAt = created,
            updatedAt = created,
        )
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
    }
}
