package com.example.iha_panel.data

import com.example.iha_panel.domain.AviationMath
import com.example.iha_panel.domain.WeatherBundle
import com.example.iha_panel.domain.WeatherSnapshot

class IncompleteWeatherDataException(message: String) : Exception(message)

private fun fail(field: String): Nothing =
    throw IncompleteWeatherDataException("Eksik meteoroloji alanı: $field")

private fun <T> List<T?>?.at(i: Int): T? = this?.getOrNull(i)

/**
 * API yanıtını alan modeline çevirir. Karar için zorunlu bir alan eksikse
 * [IncompleteWeatherDataException] fırlatır: veri yoksa karar da yoktur.
 * Tahmin saatlerinde eksik alan varsa o saat atlanır.
 */
fun WeatherResponse.toBundle(): WeatherBundle {
    val cur = current ?: fail("current")
    val h = hourly

    val temp = cur.temperature ?: fail("sıcaklık")
    val humidity = cur.humidity ?: h?.humidity.at(0)
    val dew = h?.dewPoint.at(0)
        ?: humidity?.let { AviationMath.dewPointC(temp, it) }
        ?: fail("çiğ noktası")

    val currentSnapshot = WeatherSnapshot(
        timeEpochSec = cur.time ?: fail("gözlem zamanı"),
        temperatureC = temp,
        dewPointC = dew,
        precipitationMm = cur.precipitation ?: fail("yağış"),
        weatherCode = cur.weatherCode ?: fail("hava kodu"),
        pressureHpa = cur.pressure ?: fail("basınç"),
        visibilityM = cur.visibility ?: h?.visibility.at(0) ?: fail("görüş"),
        windSpeedKmh = cur.windSpeed ?: fail("rüzgâr hızı"),
        windGustKmh = cur.windGusts ?: fail("hamle"),
        windDirectionDeg = cur.windDirection ?: fail("rüzgâr yönü"),
        windAloftKmh = h.aloftAt(0),
        cloudCoverLowPct = h?.cloudCoverLow.at(0),
        isDay = (cur.isDay ?: 1) == 1
    )

    val forecast = h?.time.orEmpty().indices.mapNotNull { h?.snapshotAt(it) }
    return WeatherBundle(currentSnapshot, forecast)
}

private fun HourlyWeather?.aloftAt(i: Int): Double? =
    listOfNotNull(this?.windSpeed80.at(i), this?.windSpeed120.at(i), this?.windSpeed180.at(i)).maxOrNull()

private fun HourlyWeather.snapshotAt(i: Int): WeatherSnapshot? {
    val time = time.at(i) ?: return null
    val temp = temperature.at(i) ?: return null
    val dew = dewPoint.at(i) ?: humidity.at(i)?.let { AviationMath.dewPointC(temp, it) } ?: return null
    return WeatherSnapshot(
        timeEpochSec = time,
        temperatureC = temp,
        dewPointC = dew,
        precipitationMm = precipitation.at(i) ?: return null,
        weatherCode = weatherCode.at(i) ?: return null,
        pressureHpa = pressure.at(i) ?: return null,
        visibilityM = visibility.at(i) ?: return null,
        windSpeedKmh = windSpeed.at(i) ?: return null,
        windGustKmh = windGusts.at(i) ?: return null,
        windDirectionDeg = windDirection.at(i) ?: return null,
        windAloftKmh = aloftAt(i),
        cloudCoverLowPct = cloudCoverLow.at(i),
        isDay = (isDay.at(i) ?: 1) == 1
    )
}
