package com.example.iha_panel

import com.example.iha_panel.domain.FlightEvaluator
import com.example.iha_panel.ui.FlightStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class FlightEvaluatorTest {

    @Test
    fun stanag4671_windSpeedExceedsLimit_returnsNoGo() {
        val result = FlightEvaluator.evaluate(
            windSpeed = 42.0,      // Limit üstü rüzgar
            visibility = 10000.0,  // İdeal görüş
            precipitation = 0.0,   // Yağış yok
            temperature = 20.0     // İdeal sıcaklık
        )
        assertEquals(FlightStatus.NO_GO, result.status)
    }

    @Test
    fun icaoAnnex2_visibilityBelowMinimum_returnsNoGo() {
        val result = FlightEvaluator.evaluate(
            windSpeed = 10.0,
            visibility = 1200.0,   //TB2 demo asgari görüşünün (2000 m) altı
            precipitation = 0.0,
            temperature = 18.0
        )
        assertEquals(FlightStatus.NO_GO, result.status)
    }

    @Test
    fun wmo_heavyPrecipitation_returnsNoGo() {
        val result = FlightEvaluator.evaluate(
            windSpeed = 15.0,
            visibility = 8000.0,
            precipitation = 3.5,   // Şiddetli yağış
            temperature = 15.0
        )
        assertEquals(FlightStatus.NO_GO, result.status)
    }

    @Test
    fun icingHazard_subZeroTemperature_returnsCaution() {
        val result = FlightEvaluator.evaluate(
            windSpeed = 10.0,
            visibility = 9000.0,
            precipitation = 0.0,
            temperature = -3.0     // Buzlanma riski
        )
        assertEquals(FlightStatus.CAUTION, result.status)
    }

    @Test
    fun nominalConditions_returnsGo() {
        val result = FlightEvaluator.evaluate(
            windSpeed = 12.0,
            visibility = 10000.0,
            precipitation = 0.0,
            temperature = 21.0
        )
        assertEquals(FlightStatus.GO, result.status)
    }
}