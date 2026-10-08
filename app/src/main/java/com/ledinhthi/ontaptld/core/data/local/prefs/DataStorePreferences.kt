package com.ledinhthi.ontaptld.core.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStorePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AppPreferences {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val AI_DATE = stringPreferencesKey("ai_usage_date")
        val AI_COUNT = intPreferencesKey("ai_usage_count")
        val LAST_SYNC = longPreferencesKey("last_sync_at")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute_of_day")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        val SYNC_OWNER = stringPreferencesKey("sync_owner_uid")
    }

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { p ->
        ThemeMode.valueOf(p[Keys.THEME] ?: ThemeMode.SYSTEM.name)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    override val aiUsageToday: Flow<AiUsage> = dataStore.data.map { p ->
        AiUsage(date = p[Keys.AI_DATE].orEmpty(), count = p[Keys.AI_COUNT] ?: 0)
    }

    override suspend fun incrementAiUsage(today: String) {
        dataStore.edit { p ->
            if (p[Keys.AI_DATE] != today) {
                p[Keys.AI_DATE] = today
                p[Keys.AI_COUNT] = 0
            }
            p[Keys.AI_COUNT] = (p[Keys.AI_COUNT] ?: 0) + 1
        }
    }

    override val reminder: Flow<ReminderSettings> = dataStore.data.map { p ->
        ReminderSettings(
            enabled = p[Keys.REMINDER_ENABLED] ?: ReminderSettings.Default.enabled,
            minuteOfDay = p[Keys.REMINDER_MINUTE] ?: ReminderSettings.Default.minuteOfDay,
        )
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    override suspend fun setReminderTime(hour: Int, minute: Int) {
        // coerceIn: giữ giá trị trong khoảng hợp lệ dù nơi gọi truyền nhầm.
        dataStore.edit { it[Keys.REMINDER_MINUTE] = hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59) }
    }

    override val languageTag: Flow<String?> = dataStore.data.map { it[Keys.LANGUAGE_TAG] }

    override suspend fun setLanguageTag(tag: String) {
        dataStore.edit { it[Keys.LANGUAGE_TAG] = tag }
    }

    override val lastSyncAtMillis: Flow<Long> = dataStore.data.map { it[Keys.LAST_SYNC] ?: 0L }

    override suspend fun setLastSyncAt(millis: Long) {
        dataStore.edit { it[Keys.LAST_SYNC] = millis }
    }

    override suspend fun getSyncOwnerUid(): String? = dataStore.data.first()[Keys.SYNC_OWNER]

    override suspend fun getSyncCursor(collection: String): Long =
        dataStore.data.first()[cursorKey(collection)] ?: 0L

    override suspend fun setSyncCursor(collection: String, cursor: Long) {
        dataStore.edit { it[cursorKey(collection)] = cursor }
    }

    override suspend fun startSyncFor(uid: String) {
        dataStore.edit { p ->
            p.clearSync()
            p[Keys.SYNC_OWNER] = uid
        }
    }

    override suspend fun clearSyncState() {
        dataStore.edit { it.clearSync() }
    }

    private fun cursorKey(collection: String) = longPreferencesKey(SYNC_CURSOR_PREFIX + collection)

    /** Xoá mọi khoá liên quan tới đồng bộ. Các khoá mốc kéo có tên động nên phải dò theo tiền tố. */
    private fun MutablePreferences.clearSync() {
        remove(Keys.SYNC_OWNER)
        remove(Keys.LAST_SYNC)
        asMap().keys.filter { it.name.startsWith(SYNC_CURSOR_PREFIX) }.forEach { remove(it) }
    }

    private companion object {
        const val SYNC_CURSOR_PREFIX = "sync_cursor_"
    }
}
