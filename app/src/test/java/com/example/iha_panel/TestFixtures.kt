package com.example.iha_panel

import com.example.iha_panel.domain.WeatherSnapshot

/** Varsayılan: TB2 için tüm parametreleri zarf içinde olan sakin bir gün. */
fun snap(
    time: Long = 1_000_000L,
    temp: Double = 21.0,
    dew: Double = 10.0,
    precip: Double = 0.0,
    code: Int = 0,
    pressure: Double = 1013.25,
    vis: Double = 10_000.0,
    wind: Double = 12.0,
    gust: Double = wind,
    dir: Double = 270.0,
    aloft: Double? = 20.0,
    cloudLow: Double? = 10.0,
    day: Boolean = true
) = WeatherSnapshot(
    timeEpochSec = time,
    temperatureC = temp,
    dewPointC = dew,
    precipitationMm = precip,
    weatherCode = code,
    pressureHpa = pressure,
    visibilityM = vis,
    windSpeedKmh = wind,
    windGustKmh = gust,
    windDirectionDeg = dir,
    windAloftKmh = aloft,
    cloudCoverLowPct = cloudLow,
    isDay = day
)
