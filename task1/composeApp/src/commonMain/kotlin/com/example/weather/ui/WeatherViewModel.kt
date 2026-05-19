package com.example.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather.data.WeatherRepository
import com.example.weather.data.WeatherResult
import com.example.weather.data.model.Weather
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WeatherUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val current: Weather? = null,
    val savedCities: List<Weather> = emptyList(),
    val selectedCity: String? = null,
    val error: String? = null,
    val offlineNotice: Boolean = false
)

class WeatherViewModel(
    private val repo: WeatherRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    init {
        val cached = repo.cachedCities()
        if (cached.isNotEmpty()) {
            _state.update {
                it.copy(
                    savedCities = cached,
                    current = cached.first(),
                    selectedCity = cached.first().city
                )
            }
        }
    }

    fun updateQuery(q: String) {
        _state.update { it.copy(query = q, error = null) }
    }

    fun selectCity(city: String) {
        val match = _state.value.savedCities.firstOrNull { it.city.equals(city, ignoreCase = true) }
        if (match != null) {
            _state.update { it.copy(current = match, selectedCity = match.city) }
        }
    }

    fun search(city: String? = null): Job? {
        val target = (city ?: _state.value.query).trim()
        if (target.isEmpty()) {
            _state.update { it.copy(error = "Введите название города") }
            return null
        }
        _state.update { it.copy(isLoading = true, error = null, offlineNotice = false) }
        return scope.launch {
            val result = withContext(ioDispatcher) { repo.getWeather(target) }
            when (result) {
                is WeatherResult.Success -> {
                    val updatedSaved = (_state.value.savedCities
                        .filterNot { it.city.equals(result.weather.city, ignoreCase = true) }
                        + result.weather)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            current = result.weather,
                            selectedCity = result.weather.city,
                            savedCities = updatedSaved,
                            offlineNotice = result.fromCache,
                            error = null
                        )
                    }
                }
                is WeatherResult.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message,
                            offlineNotice = false
                        )
                    }
                }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
