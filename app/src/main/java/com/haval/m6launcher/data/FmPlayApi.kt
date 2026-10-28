package com.haval.m6launcher.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class FmPlayStation(
    val id: String,
    val name: String,
    val logoUrl: String,
    val streamUrl: String
)

object FmPlayApi {
    private const val API_URL = "http://109.239.133.242/stations_mobile.php"
    // ИСПОЛЬЗУЕМ ДОМЕН ДЛЯ КАРТИНОК (HTTPS, чтобы не блокировалось)
    private const val IMAGE_BASE_URL = "https://fmplay.ru/"

    private var cachedStations: List<FmPlayStation>? = null

    fun clearCache() {
        cachedStations = null
    }

    suspend fun fetchStations(context: Context): Pair<List<FmPlayStation>, String?> = withContext(Dispatchers.IO) {
        cachedStations?.let { return@withContext Pair(it, null) }

        val stations = mutableListOf<FmPlayStation>()
        try {
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }

                if (response.isBlank()) return@withContext Pair(emptyList(), "Сервер вернул пустой ответ")

                val jsonObject = JSONObject(response)
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    val stationObj = jsonObject.getJSONObject(id)

                    val name = stationObj.optString("name", "Без названия")
                    val logo = stationObj.optString("logo", "")

                    var stream = stationObj.optString("url_hi", "")
                    if (stream.isBlank()) stream = stationObj.optString("url_low", "")
                    if (stream.isBlank()) stream = stationObj.optString("url", "")

                    // Собираем полный URL картинки
                    val fullLogoUrl = if (logo.startsWith("http")) {
                        logo.replace("http://", "https://") // Принудительно HTTPS для картинок
                    } else {
                        "${IMAGE_BASE_URL}${logo}"
                    }

                    if (stream.isNotBlank()) {
                        stations.add(FmPlayStation(id, name, fullLogoUrl, stream))
                    }
                }
                cachedStations = stations
                return@withContext Pair(stations, null)
            } else {
                return@withContext Pair(emptyList(), "Ошибка сервера: $responseCode")
            }
        } catch (e: Exception) {
            return@withContext Pair(emptyList(), "Ошибка сети: ${e.localizedMessage}")
        }
    }
}