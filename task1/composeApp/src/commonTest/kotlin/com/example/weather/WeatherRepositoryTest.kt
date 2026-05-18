package com.example.weather

import com.example.weather.data.OpenWeatherMapApi
import com.example.weather.data.WeatherRepository
import com.example.weather.data.WeatherResult
import com.example.weather.data.model.Weather
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WeatherRepositoryTest {

    private val payload = """
        {
          "name":"Minsk",
          "main":{"temp":12.0,"humidity":55},
          "weather":[{"id":800,"main":"Clear","description":"ясно","icon":"01d"}],
          "wind":{"speed":2.0},
          "dt":1700000000
        }
    """.trimIndent()

    private fun jsonEngine(body: String) = MockEngine { _ ->
        respond(
            content = ByteReadChannel(body),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }

    @Test
    fun fetches_weather_and_caches_it() = runTest {
        val cache = InMemoryWeatherCache()
        val api = OpenWeatherMapApi(jsonEngine(payload), apiKey = "X")
        val repo = WeatherRepository(api, cache)

        val result = repo.getWeather("Minsk")
        assertTrue(result is WeatherResult.Success)
        assertEquals("Minsk", result.weather.city)
        assertEquals(false, result.fromCache)
        assertEquals("Minsk", cache.load("Minsk")?.city)
    }

    @Test
    fun returns_cached_data_when_network_fails() = runTest {
        val cache = InMemoryWeatherCache().also {
            it.save(
                Weather("Minsk", 1.0, "снег", "13d", 80, 3.0, 0L)
            )
        }
        val api = OpenWeatherMapApi(
            MockEngine { respondError(HttpStatusCode.InternalServerError) },
            apiKey = "X"
        )
        val repo = WeatherRepository(api, cache)

        val result = repo.getWeather("Minsk")
        assertTrue(result is WeatherResult.Success)
        assertTrue(result.fromCache)
        assertEquals(1.0, result.weather.temperature)
    }

    @Test
    fun returns_failure_when_no_cache_and_network_down() = runTest {
        val api = OpenWeatherMapApi(
            MockEngine { respondError(HttpStatusCode.NotFound) },
            apiKey = "X"
        )
        val repo = WeatherRepository(api, InMemoryWeatherCache())
        val result = repo.getWeather("Atlantis")
        assertTrue(result is WeatherResult.Failure)
    }

    @Test
    fun rejects_blank_city() = runTest {
        val api = OpenWeatherMapApi(jsonEngine(payload), apiKey = "X")
        val repo = WeatherRepository(api, InMemoryWeatherCache())
        val result = repo.getWeather("   ")
        assertTrue(result is WeatherResult.Failure)
    }

    @Test
    fun trims_city_name_before_request() = runTest {
        val seen = mutableListOf<String>()
        val engine = MockEngine { request ->
            seen += request.url.parameters["q"].orEmpty()
            respond(
                content = ByteReadChannel(payload),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val repo = WeatherRepository(OpenWeatherMapApi(engine, apiKey = "X"), InMemoryWeatherCache())
        repo.getWeather("  Minsk  ")
        assertEquals(listOf("Minsk"), seen)
    }

    @Test
    fun cachedCities_returns_repository_cache() = runTest {
        val cache = InMemoryWeatherCache().also {
            it.save(Weather("Brest", 4.0, "облачно", "04d", 70, 2.0, 0L))
        }
        val api = OpenWeatherMapApi(jsonEngine(payload), apiKey = "X")
        val repo = WeatherRepository(api, cache)
        val cities = repo.cachedCities()
        assertEquals(1, cities.size)
        assertNotNull(cities.firstOrNull { it.city == "Brest" })
    }
}
