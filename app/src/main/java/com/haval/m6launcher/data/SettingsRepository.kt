package com.haval.m6launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

data class UserSettings(
    val isDarkTheme: Boolean = true,
    val isFullScreen: Boolean = true,
    val contentPadding: Int = 16,
    val yandexMapApiKey: String = "",
    val homeDoubleTapType: String = "none",
    val homeDoubleTapValue: String = "",
    val homeLongPressType: String = "none",
    val homeLongPressValue: String = "",
    // НОВОЕ ПОЛЕ: тема карты ("light", "dark", "auto")
    val mapTheme: String = "auto"
)

class SettingsRepository(private val context: Context) {
    companion object {
        val THEME_KEY = booleanPreferencesKey("is_dark_theme")
        val FULLSCREEN_KEY = booleanPreferencesKey("is_fullscreen")
        val PADDING_KEY = intPreferencesKey("content_padding")
        val YANDEX_KEY = stringPreferencesKey("yandex_api_key")
        val HOME_DOUBLE_TAP_TYPE = stringPreferencesKey("home_double_tap_type")
        val HOME_DOUBLE_TAP_VALUE = stringPreferencesKey("home_double_tap_value")
        val HOME_LONG_PRESS_TYPE = stringPreferencesKey("home_long_press_type")
        val HOME_LONG_PRESS_VALUE = stringPreferencesKey("home_long_press_value")
        // НОВЫЙ КЛЮЧ
        val MAP_THEME_KEY = stringPreferencesKey("map_theme")
    }

    val userSettings = context.dataStore.data.map { preferences ->
        UserSettings(
            isDarkTheme = preferences[THEME_KEY] ?: true,
            isFullScreen = preferences[FULLSCREEN_KEY] ?: true,
            contentPadding = preferences[PADDING_KEY] ?: 16,
            yandexMapApiKey = preferences[YANDEX_KEY] ?: "",
            homeDoubleTapType = preferences[HOME_DOUBLE_TAP_TYPE] ?: "none",
            homeDoubleTapValue = preferences[HOME_DOUBLE_TAP_VALUE] ?: "",
            homeLongPressType = preferences[HOME_LONG_PRESS_TYPE] ?: "none",
            homeLongPressValue = preferences[HOME_LONG_PRESS_VALUE] ?: "",
            mapTheme = preferences[MAP_THEME_KEY] ?: "auto"
        )
    }

    suspend fun updateSettings(settings: UserSettings) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = settings.isDarkTheme
            preferences[FULLSCREEN_KEY] = settings.isFullScreen
            preferences[PADDING_KEY] = settings.contentPadding
            preferences[YANDEX_KEY] = settings.yandexMapApiKey
            preferences[HOME_DOUBLE_TAP_TYPE] = settings.homeDoubleTapType
            preferences[HOME_DOUBLE_TAP_VALUE] = settings.homeDoubleTapValue
            preferences[HOME_LONG_PRESS_TYPE] = settings.homeLongPressType
            preferences[HOME_LONG_PRESS_VALUE] = settings.homeLongPressValue
            preferences[MAP_THEME_KEY] = settings.mapTheme
        }
    }
}