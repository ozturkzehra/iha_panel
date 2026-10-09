package com.example.iha_panel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iha_panel.data.IncompleteWeatherDataException
import com.example.iha_panel.data.WeatherRepository
import com.example.iha_panel.domain.MissionBriefing
import com.example.iha_panel.domain.MissionLocation
import com.example.iha_panel.domain.MissionLocations
import com.example.iha_panel.domain.MissionPlanner
import com.example.iha_panel.domain.MissionPointRole
import com.example.iha_panel.domain.PointWeather
import com.example.iha_panel.domain.UavPlatform
import com.example.iha_panel.domain.WeatherBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WeatherUiState(
    val isLoading: Boolean = true,
    val selectedPlatform: UavPlatform = UavPlatform.TB2,
    val selectedRole: MissionPointRole = MissionPointRole.LAUNCH,
    val launchLocation: MissionLocation = MissionLocations.ISTANBUL,
    /** null = "Kalkışla aynı" */
    val areaLocation: MissionLocation? = null,
    val recoveryLocation: MissionLocation? = null,
    val runwayHeadingText: String = "",
    val runwayHeadingDeg: Int? = null,
    val runwayHeadingInvalid: Boolean = false,
    val briefing: MissionBriefing? = null,
    val lastObservation: String = "--:--",
    val lastRefresh: String = "--:--:--",
    val errorMessage: String? = null,

) {
    val hasData: Boolean get() = briefing != null
}

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository(),
    private val nowEpochSec: () -> Long = { System.currentTimeMillis() / 1000 }
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var initialLocationRequested = false

    /** Ham veri saklanır; platform/pist değişince ağ isteği atmadan yeniden değerlendirilir. */
    private var rawByRole: Map<MissionPointRole, PointWeather> = emptyMap()
    private var missingRoles: Set<MissionPointRole> = emptySet()

    /** Ekran ilk açıldığında konumu bir kez istemek için (dönme/yeniden kompozisyonda tekrar etmez). */
    fun shouldRequestInitialLocation(): Boolean {
        if (initialLocationRequested) return false
        initialLocationRequested = true
        return true
    }

    fun loadWeatherData(lat: Double, lon: Double, isFallback: Boolean = false) {
        val name = if (isFallback) "İstanbul (varsayılan konum)" else "Konumum (GPS)"
        _uiState.update { it.copy(launchLocation = MissionLocation(name, lat, lon)) }
        refresh()
    }

    fun retry() = refresh()

    fun onPlatformSelected(platform: UavPlatform) {
        _uiState.update { it.copy(selectedPlatform = platform) }
        recompute()
    }

    fun onRoleSelected(role: MissionPointRole) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun onAreaSelected(location: MissionLocation?) {
        _uiState.update { it.copy(areaLocation = location) }
        refresh()
    }

    fun onRecoverySelected(location: MissionLocation?) {
        _uiState.update { it.copy(recoveryLocation = location) }
        refresh()
    }

    fun onRunwayHeadingChanged(text: String) {
        val digits = text.filter { it.isDigit() }.take(3)
        val value = digits.toIntOrNull()
        val heading = value?.takeIf { it in 0..360 }?.rem(360)
        _uiState.update {
            it.copy(
                runwayHeadingText = digits,
                runwayHeadingDeg = heading,
                runwayHeadingInvalid = digits.isNotEmpty() && heading == null
            )
        }
        recompute()
    }

    private fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val state = _uiState.value
            val locations: Map<MissionPointRole, MissionLocation> = mapOf(
                MissionPointRole.LAUNCH to state.launchLocation,
                MissionPointRole.AREA to (state.areaLocation ?: state.launchLocation),
                MissionPointRole.RECOVERY to (state.recoveryLocation ?: state.launchLocation)
            )

            try {
                // Aynı koordinat için tek istek; farklı noktalar paralel çekilir.
                val results: Map<String, Result<WeatherBundle>> = coroutineScope {
                    locations.values.distinctBy { it.key }
                        .map { loc -> loc.key to async { repository.getWeather(loc.lat, loc.lon) } }
                        .map { (key, deferred) -> key to deferred.await() }
                        .toMap()
                }

                val raw = mutableMapOf<MissionPointRole, PointWeather>()
                val missing = mutableSetOf<MissionPointRole>()
                var firstError: Throwable? = null
                for ((role, loc) in locations) {
                    val result = results.getValue(loc.key)
                    val bundle = result.getOrNull()
                    if (bundle != null) {
                        raw[role] = PointWeather(role, loc.name, bundle.current, bundle.forecast)
                    } else {
                        missing += role
                        if (firstError == null) firstError = result.exceptionOrNull()
                    }
                }

                rawByRole = raw
                missingRoles = missing

                if (raw.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            briefing = null,
                            errorMessage = messageFor(firstError)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            lastRefresh = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                .format(Date(nowEpochSec() * 1000))
                        )
                    }
                    recompute()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = messageFor(e)) }
            }
        }
    }

    private fun recompute() {
        if (rawByRole.isEmpty()) return
        val state = _uiState.value
        val now = nowEpochSec()
        val clock = SimpleDateFormat("HH:mm", Locale.getDefault())
        val briefing = MissionPlanner.brief(
            points = rawByRole.values.toList(),
            missingRoles = missingRoles,
            platform = state.selectedPlatform,
            runwayHeadingDeg = state.runwayHeadingDeg,
            nowEpochSec = now,
            timeLabel = { t -> clock.format(Date(t * 1000)) }
        )
        val observed = rawByRole[MissionPointRole.LAUNCH]?.current?.timeEpochSec
            ?: rawByRole.values.first().current.timeEpochSec
        _uiState.update {
            it.copy(
                briefing = briefing,
                lastObservation = clock.format(Date(observed * 1000))
            )
        }
    }

    private fun messageFor(error: Throwable?): String = when (error) {
        is IncompleteWeatherDataException -> "Hava verisi eksik (${error.message}). Eksik veriyle karar verilmez."
        is IOException -> "İnternet bağlantısı yok veya sunucuya ulaşılamadı."
        else -> "Hava verisi alınamadı."
    }
}
