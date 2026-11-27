package com.omarchylauncher.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

data class WeatherResult(
    val tempText: String,
    val descText: String
)

object WeatherService {

    suspend fun fetchWeather(city: String): WeatherResult? = withContext(Dispatchers.IO) {
        val cleanCity = city.trim()
        if (cleanCity.isEmpty()) return@withContext null

        runCatching {
            val encoded = URLEncoder.encode(cleanCity, "UTF-8")
            val geoJson = httpGet("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json")
            val results = JSONObject(geoJson).optJSONArray("results") ?: return@withContext null
            if (results.length() == 0) return@withContext null

            val first = results.getJSONObject(0)
            val lat = first.getDouble("latitude")
            val lon = first.getDouble("longitude")
            val resolvedName = first.optString("name", cleanCity)

            val weatherJson = httpGet(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code&timezone=auto"
            )
            val current = JSONObject(weatherJson).getJSONObject("current")
            val temp = current.getDouble("temperature_2m").roundToInt()
            val code = current.optInt("weather_code", 0)

            WeatherResult(
                tempText = "${temp}°C",
                descText = "$resolvedName • ${wmoToLabel(code)}"
            )
        }.getOrNull()
    }

    private fun httpGet(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
        }
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun wmoToLabel(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67 -> "Rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Showers"
        85, 86 -> "Snow Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Fair"
    }
}
