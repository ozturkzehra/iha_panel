package com.example.iha_panel

import com.example.iha_panel.domain.FlightEvaluator
import com.example.iha_panel.domain.UavPlatform
import com.example.iha_panel.ui.FlightStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class FlightEvaluatorTest {

    @Test
    fun nominalConditions_returnsGo() {
        val result = FlightEvaluator.evaluate(
            windSpeed = 12.0,
            visibility = 10000.0,
            precipitation = 0.0,
            temperature = 21.0,
            platform = UavPlatform.TB2
        )
        assertEquals(FlightStatus.GO, result.status)
    }

    @Test
    fun windSpeedExceedsLimit_returnsNoGo() {
        val result = FlightEvaluator.evaluate(
            windSpeed = 42.0,      // TB2 demo limiti (38.0) üstü
            visibility = 10000.0,
            precipitation = 0.0,
            temperature = 20.0,
            platform = UavPlatform.TB2
        )
        assertEquals(FlightStatus.NO_GO, result.status)
    }

    @Test
    fun windSpeedInCautionRange_returnsCaution() {
        val result = FlightEvaluator.evaluate(
            windSpeed = 32.0,      // 30.0 - 38.0 km/h arası dikkat eşiği
            visibility = 10000.0,
            precipitation = 0.0,
            temperature = 20.0,
            platform = UavPlatform.TB2
        )
        assertEquals(FlightStatus.CAUTION, result.status)
    }

    @Test
    fun visibilityBelowMinimum_returnsNoGo() {
        val result = FlightEvaluator.evaluate(
            windSpeed = 10.0,
            visibility = 1200.0,   // TB2 demo asgari görüşünün (2000 m) altı
            precipitation = 0.0,
            temperature = 18.0,
            platform = UavPlatform.TB2
        )
        assertEquals(FlightStatus.NO_GO, result.status)
    }

    @Test
    fun heavyPrecipitation_returnsNoGo() {
        val result = FlightEvaluator.evaluate(
            windSpeed = 15.0,
            visibility = 8000.0,
            precipitation = 6.0,   // Eşik üstü şiddetli yağış
            temperature = 15.0,
            platform = UavPlatform.TB2
        )
        assertEquals(FlightStatus.NO_GO, result.status)
    }

    @Test
    fun icingHazard_subZeroTemperature_returnsCaution() {
        val result = FlightEvaluator.evaluate(
            windSpeed = 10.0,
            visibility = 9000.0,
            precipitation = 0.0,
            temperature = -3.0,    // Kodundaki kural: Sıfırın altı sıcaklık CAUTION üretir
            platform = UavPlatform.TB2
        )
        assertEquals(FlightStatus.CAUTION, result.status)
    }

    @Test
    fun higherTolerancePlatform_handlesHigherWind() {
        val result = FlightEvaluator.evaluate(
            windSpeed = 45.0,      // TB2 için NO_GO ama KIZILELMA (65 km/h) için GO
            visibility = 10000.0,
            precipitation = 0.0,
            temperature = 15.0,
            platform = UavPlatform.KIZILELMA
        )
        assertEquals(FlightStatus.GO, result.status)
    }
}