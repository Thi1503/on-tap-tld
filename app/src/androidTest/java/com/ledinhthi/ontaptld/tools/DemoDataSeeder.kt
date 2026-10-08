package com.ledinhthi.ontaptld.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ledinhthi.ontaptld.core.data.local.db.ALL_MIGRATIONS
import com.ledinhthi.ontaptld.core.data.local.db.AppDatabase
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.FlashcardEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteEntity
import com.ledinhthi.ontaptld.feature.deck.data.local.NoteImageStore
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * CÔNG CỤ, không phải test: nạp một bộ dữ liệu mẫu vào database của bản debug đang cài trên máy /
 * emulator — để chụp màn hình, quay demo, và thử đồng bộ.
 *
 * Dữ liệu gồm 4 bộ thẻ (tên và màu như bản thiết kế), 37 thẻ có nội dung thật, đủ các tình
 * huống: thẻ mới chưa ôn, thẻ đã ôn nay đến hạn, thẻ đã thuộc hẹn nhiều ngày sau, thẻ gõ tay và
 * thẻ AI. Bộ "Sinh học 12" có một ghi chú kèm ẢNH (do công cụ này tự vẽ), để mặt đáp án có đoạn
 * trích và màn "Xem cả ảnh" tô sáng đúng dòng — không cần chụp ảnh hay tốn lượt AI.
 *
 * Bị bỏ qua trong các lần chạy test thông thường; chỉ chạy khi truyền `-e seedDemo true`:
 *
 * ```
 * ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
 * adb install -r app/build/outputs/apk/debug/app-debug.apk
 * adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
 * adb shell am instrument -w -e seedDemo true \
 *   -e class com.ledinhthi.ontaptld.tools.DemoDataSeeder \
 *   com.ledinhthi.ontaptld.test/androidx.test.runner.AndroidJUnitRunner
 * ```
 * (Đừng dùng `./gradlew connectedDebugAndroidTest`: lệnh đó gỡ app khi chạy xong.)
 *
 * An toàn với dữ liệu thật: nếu database đã có bộ thẻ nào thì dừng, không ghi thêm / ghi đè.
 * Đang đăng nhập thì lần mở app kế tiếp sẽ tự đẩy bộ dữ liệu này lên đám mây (ảnh thì không).
 */
@RunWith(AndroidJUnit4::class)
class DemoDataSeeder {

    /**
     * Một thẻ mẫu. [noteLine] = thẻ AI này được rút từ dòng thứ mấy của ghi chú (đếm từ 1);
     * null = thẻ gõ tay.
     */
    private data class DemoCard(val question: String, val answer: String, val noteLine: Int? = null)

    /**
     * [dueCount] thẻ đầu tiên của bộ là thẻ "cần ôn hôm nay". [noteLines] = các dòng của trang
     * ghi chú đã "chụp" (dòng đầu là tiêu đề); rỗng nếu bộ không có ghi chú nào.
     */
    private data class DemoDeck(
        val name: String,
        val colorHex: String,
        val dueCount: Int,
        val cards: List<DemoCard>,
        val noteLines: List<String> = emptyList(),
    )

    // Thứ tự khai báo = thứ tự hiển thị trên Home (bộ đầu có updatedAt mới nhất).
    private val demoDecks = listOf(
        DemoDeck(
            name = "IELTS Vocabulary", colorHex = "#4F46E5", dueCount = 5,
            cards = listOf(
                DemoCard("ubiquitous (adj)", "Có mặt ở khắp nơi; phổ biến đến mức đâu cũng thấy."),
                DemoCard("mitigate (v)", "Làm giảm nhẹ mức độ nghiêm trọng của một vấn đề."),
                DemoCard(
                    "Phân biệt affect và effect?",
                    "Affect là động từ (gây ảnh hưởng); effect là danh từ (kết quả, tác động).",
                ),
                DemoCard("a double-edged sword", "Con dao hai lưỡi: vừa có lợi vừa có hại."),
                DemoCard("inevitable (adj)", "Không thể tránh khỏi; chắc chắn sẽ xảy ra."),
                DemoCard("to take something for granted", "Coi điều gì là hiển nhiên, không biết trân trọng."),
                DemoCard("sustainable (adj)", "Bền vững; duy trì được lâu dài mà không làm cạn kiệt tài nguyên."),
                DemoCard(
                    "Phân biệt economic và economical?",
                    "Economic: thuộc về kinh tế. Economical: tiết kiệm, ít tốn kém.",
                ),
                DemoCard("deteriorate (v)", "Xấu đi, xuống cấp dần theo thời gian."),
                DemoCard("on the verge of", "Sắp sửa; đang ở ngay sát một thay đổi lớn."),
                DemoCard("controversial (adj)", "Gây tranh cãi; có nhiều ý kiến trái chiều."),
                DemoCard("to bridge the gap", "Thu hẹp khoảng cách giữa hai nhóm hoặc hai tình trạng."),
            ),
        ),
        DemoDeck(
            name = "Sinh học 12 – Di truyền", colorHex = "#0D9488", dueCount = 4,
            noteLines = listOf(
                "Nhân đôi ADN",
                "Diễn ra ở pha S của kì trung gian",
                "Nguyên tắc bổ sung: A–T, G–X",
                "Nguyên tắc bán bảo toàn: giữ lại một mạch cũ",
                "ADN pôlimeraza tổng hợp mạch mới theo chiều 5'→3'",
                "Mạch chậm tổng hợp gián đoạn thành các đoạn Okazaki",
                "Enzim ligaza nối các đoạn Okazaki lại",
            ),
            cards = listOf(
                DemoCard("Nhân đôi ADN diễn ra ở pha nào của chu kì tế bào?", "Pha S của kì trung gian.", noteLine = 2),
                DemoCard("Nguyên tắc bổ sung trong nhân đôi ADN là gì?", "A liên kết với T, G liên kết với X.", noteLine = 3),
                DemoCard(
                    "Thế nào là nguyên tắc bán bảo toàn?",
                    "Mỗi ADN con giữ lại một mạch của ADN mẹ và có một mạch mới tổng hợp.",
                    noteLine = 4,
                ),
                DemoCard("ADN pôlimeraza tổng hợp mạch mới theo chiều nào?", "Chiều 5'→3'.", noteLine = 5),
                DemoCard(
                    "Đoạn Okazaki là gì?",
                    "Các đoạn ADN ngắn được tổng hợp gián đoạn trên mạch chậm.",
                    noteLine = 6,
                ),
                DemoCard("Enzim nào nối các đoạn Okazaki lại với nhau?", "Enzim ligaza.", noteLine = 7),
                DemoCard("Gen là gì?", "Một đoạn ADN mang thông tin mã hoá một sản phẩm (ARN hoặc chuỗi pôlipeptit)."),
                DemoCard(
                    "Mã di truyền có tính thoái hoá nghĩa là gì?",
                    "Nhiều bộ ba khác nhau cùng mã hoá một axit amin.",
                ),
                DemoCard("Bộ ba mở đầu trên mARN là gì?", "AUG — mã hoá mêtiônin ở sinh vật nhân thực."),
            ),
        ),
        DemoDeck(
            name = "Lịch sử Việt Nam", colorHex = "#DB2777", dueCount = 2,
            cards = listOf(
                DemoCard(
                    "Chiến thắng Bạch Đằng của Ngô Quyền diễn ra năm nào?",
                    "Năm 938, chấm dứt hơn một nghìn năm Bắc thuộc.",
                ),
                DemoCard("Ai dời đô từ Hoa Lư về Thăng Long, vào năm nào?", "Lý Công Uẩn (Lý Thái Tổ), năm 1010."),
                DemoCard(
                    "Nhà Trần ba lần đánh thắng quân xâm lược nào?",
                    "Quân Mông – Nguyên, vào các năm 1258, 1285 và 1288.",
                ),
                DemoCard("Khởi nghĩa Lam Sơn do ai lãnh đạo?", "Lê Lợi, từ năm 1418 đến năm 1427."),
                DemoCard("Tác giả “Bình Ngô đại cáo” là ai?", "Nguyễn Trãi, viết năm 1428."),
                DemoCard(
                    "Chiến thắng Ngọc Hồi – Đống Đa diễn ra khi nào?",
                    "Tết Kỷ Dậu 1789, Quang Trung đại phá quân Thanh.",
                ),
                DemoCard(
                    "Cách mạng tháng Tám thành công năm nào?",
                    "Năm 1945; ngày 2/9/1945 nước Việt Nam Dân chủ Cộng hoà ra đời.",
                ),
                DemoCard("Chiến thắng Điện Biên Phủ diễn ra ngày nào?", "Ngày 7/5/1954."),
            ),
        ),
        DemoDeck(
            name = "Kotlin Coroutines", colorHex = "#0284C7", dueCount = 0,
            cards = listOf(
                DemoCard(
                    "suspend function là gì?",
                    "Hàm có thể tạm dừng rồi chạy tiếp sau, mà không chặn luồng đang chạy nó.",
                ),
                DemoCard(
                    "launch khác async ở điểm nào?",
                    "launch trả về Job, không có kết quả; async trả về Deferred, lấy kết quả bằng await().",
                ),
                DemoCard("Dispatchers.IO dùng cho việc gì?", "Các việc phải chờ đọc / ghi: mạng, file, database."),
                DemoCard(
                    "viewModelScope bị huỷ khi nào?",
                    "Khi ViewModel bị xoá (onCleared); mọi coroutine trong nó bị huỷ theo.",
                ),
                DemoCard(
                    "StateFlow khác SharedFlow ở điểm nào?",
                    "StateFlow luôn giữ một giá trị hiện tại và phát lại nó cho người mới nghe; SharedFlow thì không bắt buộc.",
                ),
                DemoCard(
                    "Structured concurrency là gì?",
                    "Coroutine con sống trong phạm vi của coroutine cha: cha bị huỷ thì con bị huỷ, con lỗi thì cha biết.",
                ),
                DemoCard(
                    "withContext dùng để làm gì?",
                    "Chuyển sang dispatcher khác để chạy một khối lệnh, rồi trả kết quả về chỗ gọi.",
                ),
                DemoCard(
                    "Vì sao không nên bắt CancellationException rồi bỏ qua?",
                    "Vì đó là tín hiệu huỷ; nuốt mất nó thì coroutine không dừng được đúng cách.",
                ),
            ),
        ),
    )

    @Test
    fun seed() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Bỏ qua: chỉ chạy khi truyền -e seedDemo true", args.getString("seedDemo") == "true")

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notesDirectory = File(context.filesDir, NoteImageStore.DIRECTORY).apply { mkdirs() }
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

                // Bộ có ghi chú: vẽ ảnh trang ghi chú, lưu ghi chú, và nhớ vị trí từng dòng trên ảnh.
                var noteId: String? = null
                var lineBoxes = emptyList<Box>()
                if (demo.noteLines.isNotEmpty()) {
                    noteId = UUID.randomUUID().toString()
                    val image = File(notesDirectory, "$noteId.jpg")
                    lineBoxes = drawNoteImage(image, demo.noteLines)
                    db.noteDao().upsert(
                        NoteEntity(
                            id = noteId,
                            deckId = deckId,
                            imagePath = image.absolutePath,
                            ocrText = demo.noteLines.joinToString("\n"),
                            createdAt = stamp - CREATED_DAYS_AGO * DAY,
                            updatedAt = stamp - CREATED_DAYS_AGO * DAY,
                        ),
                    )
                }

                db.flashcardDao().upsertAll(
                    demo.cards.mapIndexed { index, card -> card(card, demo, deckId, noteId, lineBoxes, index, stamp, now) },
                )
            }
        } finally {
            db.close()
        }
    }

    private fun card(
        demo: DemoCard,
        deck: DemoDeck,
        deckId: String,
        noteId: String?,
        lineBoxes: List<Box>,
        index: Int,
        stamp: Long,
        now: Long,
    ): FlashcardEntity {
        // createdAt giảm dần theo index -> thẻ khai báo trước đứng đầu danh sách của bộ.
        // Tạo từ ba tuần trước: đủ xa để các lần "đã ôn" bên dưới đều nằm SAU lúc tạo thẻ.
        val created = stamp - CREATED_DAYS_AGO * DAY - index * 1000L
        val box = demo.noteLine?.let { lineBoxes.getOrNull(it - 1) }
        val base = FlashcardEntity(
            id = UUID.randomUUID().toString(),
            deckId = deckId,
            noteId = if (demo.noteLine != null) noteId else null,
            source = if (demo.noteLine != null) FlashcardSource.AI else FlashcardSource.MANUAL,
            question = demo.question,
            answer = demo.answer,
            sourceBoxLeft = box?.left,
            sourceBoxTop = box?.top,
            sourceBoxRight = box?.right,
            sourceBoxBottom = box?.bottom,
            sourceLine = demo.noteLine,
            dueDate = now,
            createdAt = created,
            updatedAt = created,
        )
        return when {
            // Nửa đầu số thẻ đến hạn: thẻ MỚI, chưa ôn lần nào.
            index < (deck.dueCount + 1) / 2 -> base.copy(dueDate = now - HOUR)

            // Nửa sau: đã ôn vài ngày trước, hôm nay tới hẹn ôn lại.
            index < deck.dueCount -> {
                val reviewed = now - 3 * DAY - 2 * HOUR
                base.copy(
                    interval = 3, repetitions = 2, easeFactor = 2.36,
                    dueDate = now - 2 * HOUR, lastReviewedAt = reviewed, updatedAt = reviewed,
                )
            }

            // Còn lại: thẻ đã thuộc ở nhiều mức, hẹn từ ngày mai tới hơn hai tuần nữa.
            else -> {
                val step = LearnedSteps[index % LearnedSteps.size]
                val reviewed = now - step.reviewedDaysAgo * DAY - index * 60_000L
                base.copy(
                    interval = step.interval, repetitions = step.repetitions, easeFactor = step.easeFactor,
                    dueDate = now + step.dueInDays * DAY, lastReviewedAt = reviewed, updatedAt = reviewed,
                )
            }
        }
    }

    private data class LearnedStep(
        val interval: Int,
        val repetitions: Int,
        val easeFactor: Double,
        val reviewedDaysAgo: Int,
        val dueInDays: Int,
    )

    /** Vùng của một dòng chữ trên ảnh, tính theo tỉ lệ của ảnh (0..1) — đúng kiểu `sourceBox`. */
    private data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /**
     * Vẽ một "trang ghi chú" chữ in lên nền giấy rồi lưu thành file JPEG, thay cho ảnh chụp thật.
     * Trả về vùng của từng dòng (cùng thứ tự với [lines]) để gắn vào thẻ làm vùng tô sáng.
     */
    private fun drawNoteImage(file: File, lines: List<String>): List<Box> {
        val width = 1400
        val height = (2 * PAGE_PADDING + lines.size * LINE_HEIGHT).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap).apply { drawColor(0xFFFDFCF7.toInt()) }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1F2937.toInt()
            textSize = 42f
        }
        val titlePaint = Paint(bodyPaint).apply {
            textSize = 56f
            isFakeBoldText = true
        }
        val boxes = lines.mapIndexed { index, text ->
            val paint = if (index == 0) titlePaint else bodyPaint
            // `baseline` = đường chân chữ; chữ được vẽ "ngồi" trên đường này.
            val baseline = PAGE_PADDING + (index + 1) * LINE_HEIGHT - 36f
            canvas.drawText(text, PAGE_PADDING, baseline, paint)
            // fontMetrics.ascent (số âm) / descent: phần chữ nhô lên trên / thò xuống dưới chân chữ.
            val metrics = paint.fontMetrics
            Box(
                left = (PAGE_PADDING - 14f) / width,
                top = (baseline + metrics.ascent - 8f) / height,
                right = ((PAGE_PADDING + paint.measureText(text) + 14f) / width).coerceAtMost(1f),
                bottom = (baseline + metrics.descent + 8f) / height,
            )
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        bitmap.recycle()
        return boxes
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
        const val CREATED_DAYS_AGO = 21
        const val PAGE_PADDING = 90f
        const val LINE_HEIGHT = 120f

        /** Các mức "đã thuộc" xoay vòng cho thẻ không đến hạn, để lịch ôn sắp tới trông tự nhiên. */
        val LearnedSteps = listOf(
            LearnedStep(interval = 1, repetitions = 1, easeFactor = 2.5, reviewedDaysAgo = 0, dueInDays = 1),
            LearnedStep(interval = 6, repetitions = 2, easeFactor = 2.6, reviewedDaysAgo = 3, dueInDays = 3),
            LearnedStep(interval = 6, repetitions = 2, easeFactor = 2.36, reviewedDaysAgo = 1, dueInDays = 5),
            LearnedStep(interval = 15, repetitions = 3, easeFactor = 2.7, reviewedDaysAgo = 6, dueInDays = 9),
            LearnedStep(interval = 16, repetitions = 3, easeFactor = 2.5, reviewedDaysAgo = 1, dueInDays = 15),
            LearnedStep(interval = 35, repetitions = 4, easeFactor = 2.6, reviewedDaysAgo = 12, dueInDays = 23),
        )
    }
}
