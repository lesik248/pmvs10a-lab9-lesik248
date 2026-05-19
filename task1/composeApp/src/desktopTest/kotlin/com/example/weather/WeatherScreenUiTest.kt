package com.example.weather

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.example.weather.data.OpenWeatherMapApi
import com.example.weather.data.WeatherRepository
import com.example.weather.ui.WeatherScreen
import com.example.weather.ui.WeatherViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class WeatherScreenUiTest {

    private val payload = """
        {
          "name":"Minsk",
          "main":{"temp":12.0,"humidity":55},
          "weather":[{"id":800,"main":"Clear","description":"ясно","icon":"01d"}],
          "wind":{"speed":2.0},
          "dt":1700000000
        }
    """.trimIndent()

    private fun api() = OpenWeatherMapApi(
        MockEngine {
            respond(
                ByteReadChannel(payload),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        },
        apiKey = "X"
    )

    @Test
    fun shows_title_and_search_controls() = runComposeUiTest {
        val vm = WeatherViewModel(
            WeatherRepository(api(), InMemoryWeatherCache()),
            Dispatchers.Unconfined,
            externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        )
        setContent { WeatherScreen(vm) }
        onNodeWithText("Погода").assertIsDisplayed()
        onNodeWithText("Найти").assertIsDisplayed()
    }

    @Test
    fun typing_into_field_and_clicking_button_renders_card() = runComposeUiTest {
        val vm = WeatherViewModel(
            WeatherRepository(api(), InMemoryWeatherCache()),
            Dispatchers.Unconfined,
            externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        )
        setContent { WeatherScreen(vm) }
        onNodeWithText("Введите город").performTextInput("Minsk")
        onNodeWithText("Найти").performClick()
        waitUntil(timeoutMillis = 5_000) { vm.state.value.current != null }
        waitForIdle()
        onNodeWithText("Minsk").assertIsDisplayed()
    }

    @Test
    fun empty_query_shows_error() = runComposeUiTest {
        val vm = WeatherViewModel(
            WeatherRepository(api(), InMemoryWeatherCache()),
            Dispatchers.Unconfined,
            externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        )
        setContent { WeatherScreen(vm) }
        onNodeWithText("Найти").performClick()
        waitForIdle()
        onAllNodesWithText("Введите название города").assertCountEquals(1)
    }

    @Test
    fun renders_temperature_with_degree_symbol() = runComposeUiTest {
        val vm = WeatherViewModel(
            WeatherRepository(api(), InMemoryWeatherCache()),
            Dispatchers.Unconfined,
            externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        )
        setContent { WeatherScreen(vm) }
        onNodeWithText("Введите город").performTextInput("Minsk")
        onNodeWithText("Найти").performClick()
        waitUntil(timeoutMillis = 5_000) { vm.state.value.current != null }
        waitForIdle()
        onNodeWithText("12°C").assertIsDisplayed()
    }
}
