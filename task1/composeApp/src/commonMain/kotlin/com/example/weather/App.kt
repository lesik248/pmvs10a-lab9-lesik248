package com.example.weather

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.weather.data.OpenWeatherMapApi
import com.example.weather.data.SettingsWeatherCache
import com.example.weather.data.WeatherRepository
import com.example.weather.ui.WeatherScreen
import com.example.weather.ui.WeatherViewModel
import com.russhwolf.settings.Settings

@Composable
fun App() {
    val vm = remember {
        val api = OpenWeatherMapApi(apiKey = BuildConfig.OPENWEATHER_API_KEY)
        val cache = SettingsWeatherCache(Settings())
        val repo = WeatherRepository(api, cache)
        WeatherViewModel(repo)
    }
    MaterialTheme(
        colorScheme = lightColorScheme()
    ) {
        WeatherScreen(vm)
    }
}
