package com.haval.m6launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
    val mapTheme: String = "auto",
    val isVoiceAssistantEnabled: Boolean = true,
    val favoriteRadioIds: Set<String> = emptySet(),
    val floatingDoubleTapType: String = "home",
    val floatingDoubleTapValue: String = "",
    val quickAccessApps: List<String> = List(9) { "" },
    val isAutoPlayEnabled: Boolean = false,

    val lastActivePlayer: String = "LOCAL",
    val lastFolderUri: String = "",
    val lastRadioStationId: String = "",
    val lastTrackIndex: Int = 0,
    val lastTrackPosition: Int = 0,
    val lastRadioStationUrl: String = "",
    val lastRadioStationName: String = "",
    // НОВОЕ ПОЛЕ
    val lastRadioStationLogo: String = ""
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
        val MAP_THEME_KEY = stringPreferencesKey("map_theme")
        val VOICE_ENABLED_KEY = booleanPreferencesKey("voice_enabled")
        val FAVORITE_RADIO_IDS = stringSetPreferencesKey("favorite_radio_ids")
        val FLOATING_DOUBLE_TAP_TYPE = stringPreferencesKey("floating_double_tap_type")
        val FLOATING_DOUBLE_TAP_VALUE = stringPreferencesKey("floating_double_tap_value")
        val QUICK_ACCESS_APPS = stringPreferencesKey("quick_access_apps")
        val AUTO_PLAY_KEY = booleanPreferencesKey("is_auto_play_enabled")

        val LAST_ACTIVE_PLAYER = stringPreferencesKey("last_active_player")
        val LAST_FOLDER_URI = stringPreferencesKey("last_folder_uri")
        val LAST_RADIO_STATION_ID = stringPreferencesKey("last_radio_station_id")
        val LAST_TRACK_INDEX = intPreferencesKey("last_track_index")
        val LAST_TRACK_POSITION = intPreferencesKey("last_track_position")
        val LAST_RADIO_STATION_URL = stringPreferencesKey("last_radio_station_url")
        val LAST_RADIO_STATION_NAME = stringPreferencesKey("last_radio_station_name")
        // НОВЫЙ КЛЮЧ
        val LAST_RADIO_STATION_LOGO = stringPreferencesKey("last_radio_station_logo")
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
            mapTheme = preferences[MAP_THEME_KEY] ?: "auto",
            isVoiceAssistantEnabled = preferences[VOICE_ENABLED_KEY] ?: true,
            favoriteRadioIds = preferences[FAVORITE_RADIO_IDS] ?: emptySet(),
            floatingDoubleTapType = preferences[FLOATING_DOUBLE_TAP_TYPE] ?: "home",
            floatingDoubleTapValue = preferences[FLOATING_DOUBLE_TAP_VALUE] ?: "",
            quickAccessApps = (preferences[QUICK_ACCESS_APPS] ?: "||||||||").split("|").let {
                if (it.size == 9) it else List(9) { "" }
            },
            isAutoPlayEnabled = preferences[AUTO_PLAY_KEY] ?: false,
            lastActivePlayer = preferences[LAST_ACTIVE_PLAYER] ?: "LOCAL",
            lastFolderUri = preferences[LAST_FOLDER_URI] ?: "",
            lastRadioStationId = preferences[LAST_RADIO_STATION_ID] ?: "",
            lastTrackIndex = preferences[LAST_TRACK_INDEX] ?: 0,
            lastTrackPosition = preferences[LAST_TRACK_POSITION] ?: 0,
            lastRadioStationUrl = preferences[LAST_RADIO_STATION_URL] ?: "",
            lastRadioStationName = preferences[LAST_RADIO_STATION_NAME] ?: "",
            // ЧИТАЕМ ЛОГОТИП
            lastRadioStationLogo = preferences[LAST_RADIO_STATION_LOGO] ?: ""
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
            preferences[VOICE_ENABLED_KEY] = settings.isVoiceAssistantEnabled
            preferences[FAVORITE_RADIO_IDS] = settings.favoriteRadioIds
            preferences[FLOATING_DOUBLE_TAP_TYPE] = settings.floatingDoubleTapType
            preferences[FLOATING_DOUBLE_TAP_VALUE] = settings.floatingDoubleTapValue
            preferences[QUICK_ACCESS_APPS] = settings.quickAccessApps.joinToString("|")
            preferences[AUTO_PLAY_KEY] = settings.isAutoPlayEnabled
            preferences[LAST_ACTIVE_PLAYER] = settings.lastActivePlayer
            preferences[LAST_FOLDER_URI] = settings.lastFolderUri
            preferences[LAST_RADIO_STATION_ID] = settings.lastRadioStationId
            preferences[LAST_TRACK_INDEX] = settings.lastTrackIndex
            preferences[LAST_TRACK_POSITION] = settings.lastTrackPosition
            preferences[LAST_RADIO_STATION_URL] = settings.lastRadioStationUrl
            preferences[LAST_RADIO_STATION_NAME] = settings.lastRadioStationName
            // СОХРАНЯЕМ ЛОГОТИП
            preferences[LAST_RADIO_STATION_LOGO] = settings.lastRadioStationLogo
        }
    }
}