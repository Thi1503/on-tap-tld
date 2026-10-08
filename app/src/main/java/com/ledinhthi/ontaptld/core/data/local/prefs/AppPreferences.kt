package com.ledinhthi.ontaptld.core.data.local.prefs

import kotlinx.coroutines.flow.Flow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AiUsage(val date: String, val count: Int)

/**
 * Cài đặt nhắc ôn hằng ngày. [minuteOfDay] = giờ nhắc tính bằng số phút kể từ 0:00 (20:00 là
 * 1200) — lưu một con số thì gọn hơn lưu riêng giờ và phút.
 */
data class ReminderSettings(val enabled: Boolean, val minuteOfDay: Int) {
    val hour: Int get() = minuteOfDay / 60
    val minute: Int get() = minuteOfDay % 60

    companion object {
        /** Chưa bật: thông báo là thứ người dùng tự chọn nhận. Giờ gợi ý 20:00 như bản thiết kế. */
        val Default = ReminderSettings(enabled = false, minuteOfDay = 20 * 60)
    }
}

interface AppPreferences {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    /** Bộ đếm số lần gọi AI theo ngày — phục vụ AiQuotaGuard (docs_tld NFR Mục 10). */
    val aiUsageToday: Flow<AiUsage>
    suspend fun incrementAiUsage(today: String)

    val reminder: Flow<ReminderSettings>
    suspend fun setReminderEnabled(enabled: Boolean)
    suspend fun setReminderTime(hour: Int, minute: Int)

    // ---- Sprint 2 ----
    val lastSyncAtMillis: Flow<Long>
    suspend fun setLastSyncAt(millis: Long)
}
