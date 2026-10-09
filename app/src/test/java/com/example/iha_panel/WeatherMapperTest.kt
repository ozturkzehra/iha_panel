package com.example.iha_panel

import com.example.iha_panel.data.CurrentWeather
import com.example.iha_panel.data.HourlyWeather
import com.example.iha_panel.data.IncompleteWeatherDataException
import com.example.iha_panel.data.WeatherResponse
import com.example.iha_panel.data.toBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherMapperTest {

    private fun current(
        time: Long? = 1000L, temp: Double? = 20.0, gust: Double? = 30.0, vis: Double? = 9000.0
    ) = CurrentWeather(
        time = time, temperature = temp, humidity = 50.0, isDay = 1, precipitation = 0.0,
        weatherCode = 0, pressure = 1010.0, visibility = vis, windSpeed = 15.0,
        windDirection = 250.0, windGusts = gust
    )

    private fun hourly(vis1: Double? = 8000.0) = HourlyWeather(
        time = listOf(900L, 4500L),
        temperature = listOf(20.0, 19.0),
        humidity = listOf(50.0, 55.0),
        dewPoint = listOf(9.0, null),
        isDay = listOf(1, 1),
        precipitation = listOf(0.0, 0.2),
        weatherCode = listOf(0, 61),
        pressure = listOf(1010.0, 1009.0),
        visibility = listOf(9000.0, vis1),
        cloudCoverLow = listOf(70.0, 20.0),
        windSpeed = listOf(15.0, 18.0),
        windDirection = listOf(250.0, 260.0),
        windGusts = listOf(30.0, 35.0),
        windSpeed80 = listOf(25.0, 28.0),
        windSpeed120 = listOf(32.0, null),
        windSpeed180 = listOf(30.0, null)
    )

    @Test fun maps_current_with_hourly_extras() {
        val b = WeatherResponse(current(), hourly()).toBundle()
        assertEquals(30.0, b.current.windGustKmh, 0.0)
        assertEquals(9.0, b.current.dewPointC, 0.0)          // saatlikten
        assertEquals(70.0, b.current.cloudCoverLowPct!!, 0.0)
        assertEquals(32.0, b.current.windAloftKmh!!, 0.0)    // 80/120/180 m'nin en büyüğü
        assertTrue(b.current.isDay)
        assertEquals(2, b.forecast.size)
    }

    @Test fun dew_point_falls_back_to_magnus_when_missing() {
        val b = WeatherResponse(current(), hourly()).toBundle()
        // saatlik 2. elemanda dew yok -> T=19, RH=55 üzerinden hesaplanır
        assertNotNull(b.forecast[1].dewPointC)
        assertEquals(9.8, b.forecast[1].dewPointC, 0.5)
    }

    @Test(expected = IncompleteWeatherDataException::class)
    fun missing_gust_fails_instead_of_assuming_zero() {
        WeatherResponse(current(gust = null), hourly()).toBundle()
    }

    @Test(expected = IncompleteWeatherDataException::class)
    fun missing_current_block_fails() {
        WeatherResponse(null, hourly()).toBundle()
    }

    @Test fun forecast_hours_with_missing_fields_are_skipped() {
        val b = WeatherResponse(current(), hourly(vis1 = null)).toBundle()
        assertEquals(1, b.forecast.size)
    }

    @Test fun works_without_hourly_block_but_leaves_optional_data_null() {
        val b = WeatherResponse(current(), null).toBundle()
        assertNull(b.current.windAloftKmh)
        assertNull(b.current.cloudCoverLowPct)
        assertFalse(b.forecast.isNotEmpty())
    }
}
