package com.ledinhthi.ontaptld.feature.reminder.domain

import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.domain.usecase.NoInputUseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.dueCutoffMillis
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Giữ cho lịch nhắc luôn khớp với cài đặt: chạy suốt đời app (gọi một lần ở `OnTapTldApp`), nghe
 * cài đặt nhắc ôn và hẹn / huỷ lịch mỗi khi người dùng bật, tắt hay đổi giờ ở màn Cài đặt.
 * Hàm này KHÔNG bao giờ tự kết thúc — nó chỉ dừng khi coroutine chứa nó bị huỷ.
 */
class KeepReminderScheduledUseCase @Inject constructor(
    private val prefs: AppPreferences,
    private val scheduler: ReminderScheduler,
) : NoInputUseCase<Unit>() {

    override suspend fun invoke() {
        var isFirst = true
        // distinctUntilChanged: bỏ qua các lần phát lặp lại cùng một giá trị (file cài đặt phát lại
        // mỗi khi BẤT KỲ mục nào đổi, vd đổi giao diện).
        prefs.reminder.distinctUntilChanged().collect { settings ->
            if (settings.enabled) {
                // Giá trị đầu tiên là cài đặt đang có lúc app vừa khởi động — KHÔNG được hẹn đè:
                // có khi chính lần nhắc đang chờ vừa đánh thức app dậy để chạy.
                val mode = if (isFirst) ReminderEnqueueMode.KeepExisting else ReminderEnqueueMode.Replace
                scheduler.schedule(settings, mode)
            } else {
                scheduler.cancel()
            }
            isFirst = false
        }
    }
}

/**
 * Việc phải làm khi tới giờ nhắc (do `ReviewReminderWorker` gọi): có thẻ đến hạn thì gửi thông
 * báo, rồi hẹn lần nhắc của ngày hôm sau.
 */
class RunReviewReminderUseCase @Inject constructor(
    private val prefs: AppPreferences,
    private val flashcards: FlashcardRepository,
    private val notifier: ReminderNotifier,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
) : NoInputUseCase<Unit>() {

    override suspend fun invoke() {
        val settings = prefs.reminder.first()
        // Người dùng vừa tắt nhắc ngay trước giờ hẹn: không báo, và không hẹn tiếp.
        if (!settings.enabled) return
        try {
            // Cùng mốc "cần ôn hôm nay" với Home, để con số trong thông báo khớp con số trong app.
            val dueCount = flashcards.observeDueCount(dueCutoffMillis(clock.nowMillis())).first()
            // Không có gì để ôn thì im lặng — thông báo "0 thẻ" chỉ làm phiền.
            if (dueCount > 0) notifier.showDueCards(dueCount)
        } finally {
            // `finally`: dù phần trên có lỗi, chuỗi nhắc hằng ngày vẫn không bị đứt.
            scheduler.schedule(settings, ReminderEnqueueMode.AfterCurrentRun)
        }
    }
}
