package com.ledinhthi.ontaptld.core.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
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

    override val lastSyncAtMillis: Flow<Long> = dataStore.data.map { it[Keys.LAST_SYNC] ?: 0L }

    override suspend fun setLastSyncAt(millis: Long) {
        dataStore.edit { it[Keys.LAST_SYNC] = millis }
    }
}
