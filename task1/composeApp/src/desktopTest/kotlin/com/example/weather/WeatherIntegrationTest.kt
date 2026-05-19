package com.example.weather

import com.example.weather.data.OpenWeatherMapApi
import com.example.weather.data.WeatherRepository
import com.example.weather.ui.WeatherViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherIntegrationTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun cityPayload(city: String, temp: Double, desc: String, icon: String) = """
        {
          "name":"$city",
          "main":{"temp":$temp,"humidity":50},
          "weather":[{"id":800,"main":"Clear","description":"$desc","icon":"$icon"}],
          "wind":{"speed":1.0},
          "dt":1700000000
        }
    """.trimIndent()

    @Test
    fun viewmodel_repository_api_pipeline_succeeds() = runTest(dispatcher) {
        val engine = MockEngine { request ->
            val city = request.url.parameters["q"] ?: "Unknown"
            respond(
                ByteReadChannel(cityPayload(city, 10.0, "ясно", "01d")),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val cache = InMemoryWeatherCache()
        val vm = WeatherViewModel(
            WeatherRepository(OpenWeatherMapApi(engine, apiKey = "K"), cache),
            ioDispatcher = dispatcher,
            externalScope = this
        )

        vm.updateQuery("Minsk")
        vm.search()
        advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state.current)
        assertEquals("Minsk", state.current?.city)
        assertEquals(10.0, state.current?.temperature)
        // Cached too
        assertEquals("Minsk", cache.load("Minsk")?.city)
    }

    @Test
    fun two_searches_populate_saved_cities() = runTest(dispatcher) {
        val engine = MockEngine { request ->
            val city = request.url.parameters["q"] ?: "Unknown"
            respond(
                ByteReadChannel(cityPayload(city, 7.0, "облачно", "04d")),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val cache = InMemoryWeatherCache()
        val vm = WeatherViewModel(
            WeatherRepository(OpenWeatherMapApi(engine, apiKey = "K"), cache),
            ioDispatcher = dispatcher,
            externalScope = this
        )

        vm.updateQuery("Minsk"); vm.search(); advanceUntilIdle()
        vm.updateQuery("Brest"); vm.search(); advanceUntilIdle()

        val cities = vm.state.value.savedCities.map { it.city }.toSet()
        assertEquals(setOf("Minsk", "Brest"), cities)
    }

    @Test
    fun offline_fallback_uses_cache_after_first_success() = runTest(dispatcher) {
        var attempts = 0
        val engine = MockEngine { request ->
            attempts++
            if (attempts == 1) {
                val city = request.url.parameters["q"] ?: "Unknown"
                respond(
                    ByteReadChannel(cityPayload(city, 3.0, "снег", "13d")),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respondError(HttpStatusCode.GatewayTimeout)
            }
        }
        val cache = InMemoryWeatherCache()
        val vm = WeatherViewModel(
            WeatherRepository(OpenWeatherMapApi(engine, apiKey = "K"), cache),
            ioDispatcher = dispatcher,
            externalScope = this
        )

        vm.updateQuery("Minsk"); vm.search(); advanceUntilIdle()
        vm.updateQuery("Minsk"); vm.search(); advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state.current)
        assertEquals(3.0, state.current?.temperature)
        assertTrue(state.offlineNotice, "Expected offline notice on second attempt")
    }

    @Test
    fun api_key_is_sent_as_appid_parameter() = runTest(dispatcher) {
        val seen = mutableListOf<String>()
        val engine = MockEngine { request ->
            seen += request.url.parameters["appid"].orEmpty()
            respond(
                ByteReadChannel(cityPayload("Minsk", 5.0, "ясно", "01d")),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val repo = WeatherRepository(OpenWeatherMapApi(engine, apiKey = "SECRET"), InMemoryWeatherCache())
        repo.getWeather("Minsk")
        assertEquals(listOf("SECRET"), seen)
    }
}
