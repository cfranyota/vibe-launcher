package com.vibelauncher.app.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/**
 * Zippopotam.us (US zip or Canadian postal code -> lat/lon) + Open-Meteo (lat/lon -> current
 * conditions). Both are free, keyless APIs - fine for a personal-use launcher.
 */
class OpenMeteoWeatherRepository : WeatherRepository {

    override suspend fun current(zipCode: String): WeatherInfo = withContext(Dispatchers.IO) {
        val (lat, lon) = geocodeZip(zipCode)
        fetchWeather(lat, lon)
    }

    /** US zips go straight to Zippopotam.us's /us/ lookup unchanged; anything else is
     *  treated as a Canadian postal code - normalized (spaces stripped, uppercased) and
     *  truncated to the 3-character FSA that Zippopotam.us's /ca/ lookup expects (e.g.
     *  "K1A" from "K1A 0B1"). */
    private fun geocodeZip(zipCode: String): Pair<Double, Double> {
        val compact = zipCode.trim().replace(" ", "").uppercase()
        val (countryPath, lookupCode) = if (compact.matches(Regex("^\\d{5}$"))) {
            "us" to compact
        } else {
            "ca" to compact.take(3)
        }
        val json = JSONObject(httpGet("https://api.zippopotam.us/$countryPath/$lookupCode"))
        val place = json.getJSONArray("places").getJSONObject(0)
        return place.getString("latitude").toDouble() to place.getString("longitude").toDouble()
    }

    private fun fetchWeather(lat: Double, lon: Double): WeatherInfo {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code&temperature_unit=fahrenheit"
        val current = JSONObject(httpGet(url)).getJSONObject("current")
        val tempF = current.getDouble("temperature_2m").roundToInt()
        val code = current.getInt("weather_code")
        return WeatherInfo(condition = conditionFor(code), tempF = tempF)
    }

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** Maps WMO weather codes (Open-Meteo's `weather_code`) to a short display label. */
    private fun conditionFor(code: Int): String = when (code) {
        0 -> "sunny"
        1, 2 -> "partly cloudy"
        3 -> "cloudy"
        45, 48 -> "foggy"
        in 51..57 -> "drizzly"
        in 61..67 -> "rainy"
        in 71..77 -> "snowy"
        in 80..82 -> "rainy"
        85, 86 -> "snowy"
        in 95..99 -> "stormy"
        else -> "cloudy"
    }
}
