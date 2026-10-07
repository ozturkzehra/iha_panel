package com.example.iha_panel.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iha_panel.data.WeatherRepository
import com.example.iha_panel.domain.FlightEvaluator
import com.example.iha_panel.domain.UavPlatform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class FlightStatus {
    GO, CAUTION, NO_GO
}

data class WeatherUiState(
    val isLoading: Boolean = true,
    val hasData: Boolean = false,
    val temperature: Double = 0.0,
    val windSpeed: Double = 0.0,
    val visibility: Double = 0.0,
    val precipitation: Double = 0.0,
    val flightStatus: FlightStatus = FlightStatus.GO,
    val evaluationReason: String = "",
    val selectedPlatform: UavPlatform = UavPlatform.TB2,
    val lastUpdated: String = "--:--",
    val errorMessage: String? = null
)

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    // 5. Madde: Devam eden ağ isteğini tutan Job referansı
    private var loadJob: Job? = null

    private var lastLat = 41.0
    private var lastLon = 29.0

    init {
        loadWeatherData()
    }

    fun onPlatformSelected(platform: UavPlatform) {
        val current = _uiState.value

        if (!current.hasData) {
            _uiState.value = current.copy(selectedPlatform = platform)
            return
        }

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

    fun retry() {
        loadWeatherData(lastLat, lastLon)
    }

    fun loadWeatherData(lat: Double = 41.0, lon: Double = 29.0) {
        lastLat = lat
        lastLon = lon

        // 5. Madde: Yeni istek geldiğinde çalışan eski işi iptal et (Race Condition önlemi)
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                hasData = false,
                errorMessage = null
            )
            try {
                val weather = repository.getWeather(lat, lon).getOrThrow()

                val evaluation = FlightEvaluator.evaluate(
                    windSpeed = weather.windSpeed,
                    visibility = weather.visibility,
                    precipitation = weather.precipitation,
                    temperature = weather.temperature,
                    platform = _uiState.value.selectedPlatform
                )

                val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasData = true,
                    temperature = weather.temperature,
                    windSpeed = weather.windSpeed,
                    visibility = weather.visibility,
                    precipitation = weather.precipitation,
                    flightStatus = evaluation.status,
                    evaluationReason = evaluation.reason,
                    lastUpdated = timeFormatter.format(Date()),
                    errorMessage = null
                )
            } catch (e: CancellationException) {
                // 5. Madde: İptal hatasını yutma, coroutine yaşam döngüsü için yeniden fırlat
                throw e
            } catch (e: Exception) {
                Log.e("Weather", "Veri alınamadı", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasData = false,
                    errorMessage = when (e) {
                        is UnknownHostException,
                        is SocketTimeoutException ->
                            "İnternet bağlantısı yok ya da zayıf. Bağlantını kontrol edip tekrar dene."
                        else ->
                            "Hava verisi alınamadı. Lütfen tekrar dene."
                    }
                )
            }
        }
    }
}