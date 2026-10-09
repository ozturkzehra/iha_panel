package com.example.iha_panel

import com.example.iha_panel.domain.AviationMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class AviationMathTest {
    @org.junit.Test fun crosswind_is_full_at_90_degrees() =
        assertEquals(30.0, AviationMath.crosswindKmh(30.0, 270.0, 180), 0.001)

    @org.junit.Test fun crosswind_is_zero_when_aligned_or_reciprocal() {
        assertEquals(0.0, AviationMath.crosswindKmh(30.0, 180.0, 180), 0.001)
        assertEquals(0.0, AviationMath.crosswindKmh(30.0, 360.0, 180), 0.001)
    }

    @org.junit.Test fun crosswind_at_30_degrees_is_half() =
        assertEquals(15.0, AviationMath.crosswindKmh(30.0, 210.0, 180), 0.001)

    @org.junit.Test fun crosswind_handles_wraparound() =
        assertEquals(AviationMath.crosswindKmh(20.0, 10.0, 350), AviationMath.crosswindKmh(20.0, 370.0, 350), 0.001)

    @org.junit.Test fun density_altitude_is_zero_in_standard_atmosphere() =
        assertEquals(0.0, AviationMath.densityAltitudeM(1013.25, 15.0), 0.5)

    @org.junit.Test fun density_altitude_rises_with_heat() =
        assertEquals(731.5, AviationMath.densityAltitudeM(1013.25, 35.0), 1.0)

    @org.junit.Test fun pressure_altitude_at_800_hpa_is_about_2000_m() =
        assertEquals(1950.0, AviationMath.pressureAltitudeM(800.0), 60.0)

    @org.junit.Test fun dew_point_matches_known_value() =
        // 20 °C ve %50 nem => ~9,3 °C
        assertEquals(9.3, AviationMath.dewPointC(20.0, 50.0)!!, 0.2)

    @org.junit.Test fun dew_point_equals_temperature_at_saturation() =
        assertEquals(12.0, AviationMath.dewPointC(12.0, 100.0)!!, 0.01)

    @org.junit.Test fun dew_point_null_for_zero_humidity() = assertNull(AviationMath.dewPointC(12.0, 0.0))

    @org.junit.Test fun ceiling_estimate_is_125_m_per_degree() =
        assertEquals(375.0, AviationMath.estimatedCeilingM(15.0, 12.0), 0.001)
}
