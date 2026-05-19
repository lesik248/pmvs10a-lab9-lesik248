package com.example.weather

import com.example.weather.data.WeatherCache
import com.example.weather.data.model.Weather

class InMemoryWeatherCache : WeatherCache {
    private val storage = linkedMapOf<String, Weather>()
    override fun save(weather: Weather) {
        storage[weather.city.lowercase()] = weather
    }
    override fun load(city: String): Weather? = storage[city.lowercase()]
    override fun loadAll(): List<Weather> = storage.values.toList()
    override fun clear() = storage.clear()
}
