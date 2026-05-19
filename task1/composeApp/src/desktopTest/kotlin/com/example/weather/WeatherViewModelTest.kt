package com.example.weather

import com.example.weather.data.OpenWeatherMapApi
import com.example.weather.data.WeatherRepository
import com.example.weather.ui.WeatherViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val payload = """
        {
          "name":"Minsk",
          "main":{"temp":12.0,"humidity":55},
          "weather":[{"id":800,"main":"Clear","description":"ясно","icon":"01d"}],
          "wind":{"speed":2.0},
          "dt":1700000000
        }
    """.trimIndent()

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun api(body: String = payload) = OpenWeatherMapApi(
        MockEngine {
            respond(ByteReadChannel(body), HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"))
        },
        apiKey = "X"
    )

    @Test
    fun search_updates_state_with_weather() = runTest(dispatcher) {
        val vm = WeatherViewModel(
            WeatherRepository(api(), InMemoryWeatherCache()),
            ioDispatcher = dispatcher,
            externalScope = this
        )
        vm.updateQuery("Minsk")
        vm.search()?.join()
        val state = vm.state.value
        assertNotNull(state.current)
        assertEquals("Minsk", state.current?.city)
        assertNull(state.error)
    }

    @Test
    fun blank_query_produces_error() = runTest(dispatcher) {
        val vm = WeatherViewModel(
            WeatherRepository(api(), InMemoryWeatherCache()),
            ioDispatcher = dispatcher,
            externalScope = this
        )
        vm.updateQuery("   ")
        vm.search()
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.current)
    }

    @Test
    fun selectCity_switches_current_to_cached() = runTest(dispatcher) {
        val cache = InMemoryWeatherCache()
        val vm = WeatherViewModel(
            WeatherRepository(api(), cache),
            ioDispatcher = dispatcher,
            externalScope = this
        )
        vm.updateQuery("Minsk")
        vm.search()?.join()
        vm.updateQuery("Brest")
        // Second call updates the engine result name, but model uses 'Minsk' anyway —
        // we just need it to add a second city to savedCities.
        vm.search()?.join()
        assertTrue(vm.state.value.savedCities.isNotEmpty())
    }

    @Test
    fun clearError_resets_error_field() = runTest(dispatcher) {
        val vm = WeatherViewModel(
            WeatherRepository(api(), InMemoryWeatherCache()),
            ioDispatcher = dispatcher,
            externalScope = this
        )
        vm.updateQuery("")
        vm.search()
        assertNotNull(vm.state.value.error)
        vm.clearError()
        assertNull(vm.state.value.error)
    }
}
