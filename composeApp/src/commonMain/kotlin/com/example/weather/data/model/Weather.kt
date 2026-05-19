package com.example.weather.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val name: String,
    val main: Main,
    val weather: List<WeatherEntry>,
    val wind: Wind,
    val dt: Long
)

@Serializable
data class Main(
    val temp: Double,
    val humidity: Int,
    @SerialName("feels_like") val feelsLike: Double = temp
)

@Serializable
data class WeatherEntry(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

@Serializable
data class Wind(
    val speed: Double
)

@Serializable
data class Weather(
    val city: String,
    val temperature: Double,
    val description: String,
    val iconCode: String,
    val humidity: Int,
    val windSpeed: Double,
    val timestamp: Long
) {
    val iconUrl: String get() = "https://openweathermap.org/img/wn/${iconCode}@2x.png"

    companion object {
        fun from(response: WeatherResponse): Weather {
            val entry = response.weather.firstOrNull()
            return Weather(
                city = response.name,
                temperature = response.main.temp,
                description = entry?.description ?: "",
                iconCode = entry?.icon ?: "01d",
                humidity = response.main.humidity,
                windSpeed = response.wind.speed,
                timestamp = response.dt
            )
        }
    }
}
