package com.ledinhthi.ontaptld.core.data.local.prefs

import kotlinx.coroutines.flow.Flow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AiUsage(val date: String, val count: Int)

interface AppPreferences {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    /** Bộ đếm số lần gọi AI theo ngày — phục vụ AiQuotaGuard (docs_tld NFR Mục 10). */
    val aiUsageToday: Flow<AiUsage>
    suspend fun incrementAiUsage(today: String)

    // ---- Sprint 2 ----
    val lastSyncAtMillis: Flow<Long>
    suspend fun setLastSyncAt(millis: Long)
}
