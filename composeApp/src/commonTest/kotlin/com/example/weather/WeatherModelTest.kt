package com.example.weather

import com.example.weather.data.model.Main
import com.example.weather.data.model.Weather
import com.example.weather.data.model.WeatherEntry
import com.example.weather.data.model.WeatherResponse
import com.example.weather.data.model.Wind
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeatherModelTest {

    private val sampleJson = """
        {
          "name": "Minsk",
          "main": { "temp": 12.4, "humidity": 60, "feels_like": 11.2 },
          "weather": [{ "id": 800, "main": "Clear", "description": "ясно", "icon": "01d" }],
          "wind": { "speed": 3.5 },
          "dt": 1700000000
        }
    """.trimIndent()

    @Test
    fun parses_openweather_payload() {
        val parsed = Json { ignoreUnknownKeys = true }
            .decodeFromString<WeatherResponse>(sampleJson)
        assertEquals("Minsk", parsed.name)
        assertEquals(60, parsed.main.humidity)
        assertEquals(3.5, parsed.wind.speed)
        assertEquals("ясно", parsed.weather.first().description)
    }

    @Test
    fun converts_response_to_weather() {
        val response = WeatherResponse(
            name = "Brest",
            main = Main(temp = 7.0, humidity = 80),
            weather = listOf(WeatherEntry(803, "Clouds", "облачно", "04d")),
            wind = Wind(speed = 2.1),
            dt = 1L
        )
        val weather = Weather.from(response)
        assertEquals("Brest", weather.city)
        assertEquals(7.0, weather.temperature)
        assertEquals("04d", weather.iconCode)
        assertTrue(weather.iconUrl.endsWith("04d@2x.png"))
    }

    @Test
    fun handles_empty_weather_list() {
        val response = WeatherResponse(
            name = "Nowhere",
            main = Main(0.0, 0),
            weather = emptyList(),
            wind = Wind(0.0),
            dt = 0L
        )
        val weather = Weather.from(response)
        assertEquals("01d", weather.iconCode)
        assertEquals("", weather.description)
    }
}
