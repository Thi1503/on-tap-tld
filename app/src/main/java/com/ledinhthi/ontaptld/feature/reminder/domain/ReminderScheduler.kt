package com.ledinhthi.ontaptld.feature.reminder.domain

import com.ledinhthi.ontaptld.core.data.local.prefs.ReminderSettings
import java.time.Instant
import java.time.ZoneId

/** Khi đã có một lịch nhắc đang chờ thì lịch mới xử lý thế nào. */
enum class ReminderEnqueueMode {
    /** Giữ lịch đang có, chỉ hẹn mới nếu chưa có lịch nào — dùng lúc app khởi động. */
    KeepExisting,

    /** Bỏ lịch cũ, hẹn lại theo cài đặt mới — dùng khi người dùng đổi giờ nhắc. */
    Replace,

    /** Hẹn lần kế tiếp SAU khi lần nhắc đang chạy kết thúc — lần nhắc tự gọi để nối sang hôm sau. */
    AfterCurrentRun,
}

/**
 * Hẹn giờ cho lần nhắc ôn kế tiếp. Tách thành interface để phần quyết định "khi nào hẹn" (các
 * use case trong gói này) test được mà không cần WorkManager của Android.
 */
interface ReminderScheduler {
    /** Hẹn MỘT lần nhắc vào lúc [ReminderSettings.minuteOfDay] gần nhất sắp tới. */
    fun schedule(settings: ReminderSettings, mode: ReminderEnqueueMode)

    fun cancel()
}

/** Gửi thông báo nhắc ôn lên thanh thông báo của máy. */
interface ReminderNotifier {
    /** Không làm gì nếu người dùng đã tắt quyền thông báo của app. */
    suspend fun showDueCards(dueCount: Int)
}

/**
 * Còn bao nhiêu mili giây nữa thì tới giờ nhắc kế tiếp. Nếu hôm nay đã qua (hoặc đang đúng) giờ
 * nhắc thì tính tới giờ đó của NGÀY MAI — nhờ vậy kết quả luôn lớn hơn 0.
 *
 * Tính theo ngày giờ của lịch (`plusDays`) chứ không cộng thẳng 24 giờ, để ngày đổi giờ mùa hè /
 * mùa đông ở các nước có áp dụng vẫn ra đúng giờ trên đồng hồ.
 */
fun millisUntilNextReminder(nowMillis: Long, minuteOfDay: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    val todayAtReminder = now.toLocalDate().atStartOfDay(zone).plusMinutes(minuteOfDay.toLong())
    val next = if (todayAtReminder.isAfter(now)) todayAtReminder else todayAtReminder.plusDays(1)
    return next.toInstant().toEpochMilli() - nowMillis
}
