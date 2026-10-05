package com.example.iha_panel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iha_panel.data.CurrentWeather
import com.example.iha_panel.data.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    object Loading : UiState

    data class Success(
        val weather: CurrentWeather,
        val status: FlightStatus,
        val reason: String,
        val lat: Double,
        val lon: Double
    ) : UiState

    data class Error(val message: String) : UiState
}

enum class FlightStatus {
    GO,
    CAUTION,
    NO_GO
}

data class FlightDecision(
    val status: FlightStatus,
    val reason: String
)

class WeatherViewModel : ViewModel() {

    private val repository = WeatherRepository()

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load(41.0, 29.0)
    }

    fun load(lat: Double, lon: Double) {
        _state.value = UiState.Loading
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getWeather(lat, lon)
                .onSuccess { weatherData ->
                    val decision = evaluate(weatherData)
                    _state.value = UiState.Success(
                        weather = weatherData,
                        status = decision.status,
                        reason = decision.reason,
                        lat = lat,
                        lon = lon
                    )
                }
                .onFailure {
                    _state.value = UiState.Error(
                        "Could not load weather data. Check your connection."
                    )
                }
        }
    }

    // Demo thresholds, NOT real aviation rules
    private fun evaluate(w: CurrentWeather): FlightDecision = when {
        w.windSpeed > 40 -> FlightDecision(FlightStatus.NO_GO, "Wind exceeds 40 km/h")
        w.precipitation > 5 -> FlightDecision(FlightStatus.NO_GO, "Precipitation exceeds 5 mm")
        w.visibility < 1000 -> FlightDecision(FlightStatus.NO_GO, "Visibility below 1000 m")
        w.windSpeed > 25 -> FlightDecision(FlightStatus.CAUTION, "Wind exceeds 25 km/h")
        w.precipitation > 1 -> FlightDecision(FlightStatus.CAUTION, "Precipitation exceeds 1 mm")
        w.visibility < 5000 -> FlightDecision(FlightStatus.CAUTION, "Visibility below 5000 m")
        else -> FlightDecision(FlightStatus.GO, "All conditions within limits")
    }
}