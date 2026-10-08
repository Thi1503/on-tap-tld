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

    /**
     * Mã ngôn ngữ người dùng chọn ở màn Ngôn ngữ ("vi" / "en"); null = chưa chọn, theo máy.
     * Chỉ là BẢN SAO cho việc nền đọc khi app đang đóng (thông báo nhắc ôn) — nơi giữ lựa chọn
     * chính thức vẫn là hệ thống, xem AppCompatLanguageManager.
     */
    val languageTag: Flow<String?>
    suspend fun setLanguageTag(tag: String)

    // ---- Đồng bộ đám mây (feature/sync) ----

    /** Lần đồng bộ thành công gần nhất; 0 = chưa đồng bộ lần nào. */
    val lastSyncAtMillis: Flow<Long>
    suspend fun setLastSyncAt(millis: Long)

    /** uid của tài khoản mà dữ liệu trên máy này đang đồng bộ cùng; null = chưa từng đồng bộ. */
    suspend fun getSyncOwnerUid(): String?

    /**
     * "Mốc kéo" của một collection: lần kéo sau chỉ hỏi đám mây những bản ghi được ghi lên SAU
     * mốc này. Tính bằng nano giây theo giờ MÁY CHỦ (không phải giờ của máy này); 0 = kéo tất cả.
     */
    suspend fun getSyncCursor(collection: String): Long
    suspend fun setSyncCursor(collection: String, cursor: Long)

    /** Bắt đầu đồng bộ với tài khoản [uid]: ghi chủ mới, quên mốc kéo và giờ đồng bộ của tài khoản cũ. */
    suspend fun startSyncFor(uid: String)

    /** Quên hết trạng thái đồng bộ (sau khi xoá toàn bộ dữ liệu trên máy). */
    suspend fun clearSyncState()
}
