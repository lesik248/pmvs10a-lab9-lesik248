package com.example.weather.data

import com.example.weather.data.model.Weather

sealed interface WeatherResult {
    data class Success(val weather: Weather, val fromCache: Boolean) : WeatherResult
    data class Failure(val message: String, val cached: Weather? = null) : WeatherResult
}

class WeatherRepository(
    private val api: WeatherApi,
    private val cache: WeatherCache
) {
    suspend fun getWeather(city: String): WeatherResult {
        if (city.isBlank()) return WeatherResult.Failure("City name is empty")
        return try {
            val response = api.fetchWeather(city.trim())
            val weather = Weather.from(response)
            cache.save(weather)
            WeatherResult.Success(weather, fromCache = false)
        } catch (e: Exception) {
            val cached = cache.load(city.trim())
            if (cached != null) {
                WeatherResult.Success(cached, fromCache = true)
            } else {
                WeatherResult.Failure(e.message ?: "Unknown error", cached = null)
            }
        }
    }

    fun cachedCities(): List<Weather> = cache.loadAll()

    fun clearCache() = cache.clear()
}
