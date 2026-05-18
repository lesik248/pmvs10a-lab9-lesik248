package com.example.weather.data

import com.example.weather.data.model.Weather
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface WeatherCache {
    fun save(weather: Weather)
    fun load(city: String): Weather?
    fun loadAll(): List<Weather>
    fun clear()
}

class SettingsWeatherCache(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : WeatherCache {

    override fun save(weather: Weather) {
        val key = keyFor(weather.city)
        settings.putString(key, json.encodeToString(weather))
        val index = (settings.getStringOrNull(INDEX_KEY) ?: "")
            .split(",")
            .filter { it.isNotBlank() && !it.equals(weather.city, ignoreCase = true) }
            .plus(weather.city)
            .joinToString(",")
        settings.putString(INDEX_KEY, index)
    }

    override fun load(city: String): Weather? {
        val raw = settings.getStringOrNull(keyFor(city)) ?: return null
        return runCatching { json.decodeFromString<Weather>(raw) }.getOrNull()
    }

    override fun loadAll(): List<Weather> {
        val cities = settings.getStringOrNull(INDEX_KEY)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: return emptyList()
        return cities.mapNotNull { load(it) }
    }

    override fun clear() {
        val cities = settings.getStringOrNull(INDEX_KEY)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        cities.forEach { settings.remove(keyFor(it)) }
        settings.remove(INDEX_KEY)
    }

    private fun keyFor(city: String): String = "weather_${city.lowercase()}"

    private fun Settings.getStringOrNull(key: String): String? =
        if (hasKey(key)) getString(key, "") else null

    companion object {
        private const val INDEX_KEY = "weather_index"
    }
}
