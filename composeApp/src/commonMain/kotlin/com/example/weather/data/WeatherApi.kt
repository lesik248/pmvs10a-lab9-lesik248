package com.example.weather.data

import com.example.weather.data.model.WeatherResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

interface WeatherApi {
    suspend fun fetchWeather(city: String): WeatherResponse
}

class OpenWeatherMapApi(
    engine: HttpClientEngine? = null,
    private val apiKey: String,
    private val baseUrl: String = DEFAULT_BASE_URL
) : WeatherApi {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client: HttpClient = if (engine != null) {
        HttpClient(engine) { install(ContentNegotiation) { json(json) } }
    } else {
        HttpClient { install(ContentNegotiation) { json(json) } }
    }

    override suspend fun fetchWeather(city: String): WeatherResponse {
        require(city.isNotBlank()) { "City must not be blank" }
        return client.get("$baseUrl/data/2.5/weather") {
            parameter("q", city)
            parameter("appid", apiKey)
            parameter("units", "metric")
            parameter("lang", "en")
        }.body()
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.openweathermap.org"
    }
}
