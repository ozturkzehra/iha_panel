package com.example.iha_panel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iha_panel.data.WeatherRepository
import com.example.iha_panel.domain.FlightEvaluator
import com.example.iha_panel.domain.UavPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class FlightStatus {
    GO, CAUTION, NO_GO
}

data class WeatherUiState(
    val isLoading: Boolean = false,
    val temperature: Double = 0.0,
    val windSpeed: Double = 0.0,
    val visibility: Double = 10000.0,
    val precipitation: Double = 0.0,
    val flightStatus: FlightStatus = FlightStatus.GO,
    val evaluationReason: String = "Sistem hazır",
    val selectedPlatform: UavPlatform = UavPlatform.TB2,
    val lastUpdated: String = "--:--",
    val errorMessage: String? = null
)

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        loadWeatherData()
    }

    fun onPlatformSelected(platform: UavPlatform) {
        val current = _uiState.value
        val evaluation = FlightEvaluator.evaluate(
            windSpeed = current.windSpeed,
            visibility = current.visibility,
            precipitation = current.precipitation,
            temperature = current.temperature,
            platform = platform
        )
        _uiState.value = current.copy(
            selectedPlatform = platform,
            flightStatus = evaluation.status,
            evaluationReason = evaluation.reason
        )
    }

    fun loadWeatherData(lat: Double = 41.0, lon: Double = 29.0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val data = repository.getWeather(lat, lon)

                // 1. Result paketinden veriyi çıkarıyoruz (getOrThrow ile)
                val weather = data.getOrThrow()
                
                // 2. WeatherModels.kt'de tanımladığınız Kotlin değişken isimlerini kullanıyoruz
                val wind = weather.windSpeed
                val vis = weather.visibility
                val precip = weather.precipitation
                val temp = weather.temperature

                val evaluation = FlightEvaluator.evaluate(
                    windSpeed = wind,
                    visibility = vis,
                    precipitation = precip,
                    temperature = temp,
                    platform = _uiState.value.selectedPlatform
                )

                // API 24 uyumlu saat biçimlendirme
                val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault() // Cihazın/bulunulan yerin aktif saat dilimi
                }
                val formattedTime = timeFormatter.format(Date())

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    temperature = temp,
                    windSpeed = wind,
                    visibility = vis,
                    precipitation = precip,
                    flightStatus = evaluation.status,
                    evaluationReason = evaluation.reason,
                    lastUpdated = formattedTime
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Telemetri verisi alınamadı: ${e.localizedMessage}"
                )
            }
        }
    }
}
