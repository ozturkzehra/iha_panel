package com.example.iha_panel.domain

/**
 * Tek bir zaman dilimi (şimdi veya tahmin saati) için değerlendirmeye girecek
 * meteorolojik durum. Birimler: km/h, °C, m, mm, hPa, derece (rüzgârın GELDİĞİ yön).
 *
 * Nullable alanlar "veri yok" demektir; karar mantığı bunları sessizce 0 saymaz,
 * ilgili kuralı atlar ve brifinge not düşer.
 */
data class WeatherSnapshot(
    val timeEpochSec: Long,
    val temperatureC: Double,
    val dewPointC: Double,
    val precipitationMm: Double,
    val weatherCode: Int,
    val pressureHpa: Double,
    val visibilityM: Double,
    val windSpeedKmh: Double,
    val windGustKmh: Double,
    val windDirectionDeg: Double,
    val windAloftKmh: Double? = null,
    val cloudCoverLowPct: Double? = null,
    val isDay: Boolean = true
)

/** Bir koordinat için "şimdi" + önümüzdeki saatlerin tahmini. */
data class WeatherBundle(
    val current: WeatherSnapshot,
    val forecast: List<WeatherSnapshot>
)
