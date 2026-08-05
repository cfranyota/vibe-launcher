package com.vibelauncher.app.data.weather

interface WeatherRepository {
    /** Throws on network/geocoding failure - callers should catch and treat as "unavailable". */
    suspend fun current(zipCode: String): WeatherInfo
}
