package com.example.weather

import com.example.weather.data.model.Weather
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeatherCacheTest {

    private fun sample(city: String, temp: Double = 10.0) = Weather(
        city = city,
        temperature = temp,
        description = "ясно",
        iconCode = "01d",
        humidity = 50,
        windSpeed = 1.0,
        timestamp = 0L
    )

    @Test
    fun saves_and_loads_by_city() {
        val cache = InMemoryWeatherCache()
        cache.save(sample("Minsk", 5.0))
        assertEquals(5.0, cache.load("Minsk")?.temperature)
    }

    @Test
    fun load_is_case_insensitive() {
        val cache = InMemoryWeatherCache()
        cache.save(sample("Minsk"))
        assertEquals("Minsk", cache.load("minsk")?.city)
    }

    @Test
    fun overwrites_existing_city() {
        val cache = InMemoryWeatherCache()
        cache.save(sample("Brest", 5.0))
        cache.save(sample("Brest", 9.0))
        assertEquals(9.0, cache.load("Brest")?.temperature)
        assertEquals(1, cache.loadAll().size)
    }

    @Test
    fun load_unknown_returns_null() {
        val cache = InMemoryWeatherCache()
        assertNull(cache.load("Atlantis"))
    }

    @Test
    fun clear_removes_everything() {
        val cache = InMemoryWeatherCache()
        cache.save(sample("Minsk"))
        cache.save(sample("Brest"))
        cache.clear()
        assertTrue(cache.loadAll().isEmpty())
    }
}
