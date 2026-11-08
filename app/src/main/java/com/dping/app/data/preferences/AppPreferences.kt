package com.dping.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.dping.app.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dping_settings")

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_NEWEST_FIRST = booleanPreferencesKey("newest_first")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_PACKET_SIZE = intPreferencesKey("packet_size")
        val KEY_INTERVAL_MS = longPreferencesKey("interval_ms")
        val KEY_RESOLVE_HOSTNAME = booleanPreferencesKey("resolve_hostname")
        val KEY_KEEP_RUNNING_BG = booleanPreferencesKey("keep_running_bg")
    }

    val themeModeFlow: Flow<AppThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[KEY_THEME]) {
            "LIGHT" -> AppThemeMode.LIGHT
            "DARK" -> AppThemeMode.DARK
            else -> AppThemeMode.DARK // Dark-first default
        }
    }

    val newestFirstFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NEWEST_FIRST] ?: true
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: "en"
    }

    val packetSizeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_PACKET_SIZE] ?: 64
    }

    val intervalMsFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_INTERVAL_MS] ?: 1000L
    }

    val resolveHostnameFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_RESOLVE_HOSTNAME] ?: false
    }

    val keepRunningBgFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_KEEP_RUNNING_BG] ?: false
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = mode.name
        }
    }

    suspend fun setNewestFirst(newestFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NEWEST_FIRST] = newestFirst
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = lang
        }
    }

    suspend fun setPacketSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PACKET_SIZE] = size
        }
    }

    suspend fun setIntervalMs(interval: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_INTERVAL_MS] = interval
        }
    }

    suspend fun setResolveHostname(resolve: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RESOLVE_HOSTNAME] = resolve
        }
    }

    suspend fun setKeepRunningBg(keep: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_KEEP_RUNNING_BG] = keep
        }
    }

    suspend fun resetDefaults() {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = AppThemeMode.DARK.name
            preferences[KEY_LANGUAGE] = "en"
            preferences[KEY_NEWEST_FIRST] = true
            preferences[KEY_PACKET_SIZE] = 64
            preferences[KEY_INTERVAL_MS] = 1000L
            preferences[KEY_RESOLVE_HOSTNAME] = false
            preferences[KEY_KEEP_RUNNING_BG] = false
        }
    }
}
