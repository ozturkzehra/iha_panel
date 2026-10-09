package com.example.iha_panel

import com.example.iha_panel.domain.FlightEvaluator
import com.example.iha_panel.domain.FlightStatus
import com.example.iha_panel.domain.RiskFactor
import com.example.iha_panel.domain.UavPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlightEvaluatorTest {

    private fun status(
        platform: UavPlatform = UavPlatform.TB2,
        heading: Int? = null,
        age: Long? = null,
        w: com.example.iha_panel.domain.WeatherSnapshot
    ) = FlightEvaluator.evaluate(w, platform, heading, age).status

    // ---- Rüzgâr: sınır değerler ----
    @Test fun nominal_conditions_are_go() {
        val r = FlightEvaluator.evaluate(snap(), UavPlatform.TB2)
        assertEquals(FlightStatus.GO, r.status)
        assertTrue(r.findings.isEmpty())
    }
    @Test fun wind_just_below_caution_band_is_go() = assertEquals(FlightStatus.GO, status(w = snap(wind = 27.99)))
    @Test fun wind_at_caution_start_is_caution() = assertEquals(FlightStatus.CAUTION, status(w = snap(wind = 28.0)))
    @Test fun wind_exactly_at_limit_is_caution() = assertEquals(FlightStatus.CAUTION, status(w = snap(wind = 38.0)))
    @Test fun wind_just_over_limit_is_no_go() = assertEquals(FlightStatus.NO_GO, status(w = snap(wind = 38.01)))
    @Test fun same_wind_differs_per_platform() {
        assertEquals(FlightStatus.NO_GO, status(UavPlatform.MINI, w = snap(wind = 30.0, aloft = 20.0)))
        assertEquals(FlightStatus.GO, status(UavPlatform.KIZILELMA, w = snap(wind = 30.0, aloft = 20.0)))
    }

    // ---- Hamle ----
    @Test fun gust_over_limit_is_no_go_even_if_mean_wind_is_calm() {
        val r = FlightEvaluator.evaluate(snap(wind = 15.0, gust = 52.0), UavPlatform.TB2)
        assertEquals(FlightStatus.NO_GO, r.status)
        assertTrue(r.findings.any { it.factor == RiskFactor.GUST })
    }
    @Test fun gust_in_caution_band_is_caution() = assertEquals(FlightStatus.CAUTION, status(w = snap(wind = 15.0, gust = 42.0)))

    // ---- Yan rüzgâr ----
    @Test fun crosswind_over_limit_is_no_go() {
        // Rüzgâr 270°'den 35 km/h, pist 180° => 90° açı, yan bileşen 35 > 24
        val r = FlightEvaluator.evaluate(snap(wind = 35.0, dir = 270.0), UavPlatform.TB2, runwayHeadingDeg = 180)
        assertTrue(r.findings.any { it.factor == RiskFactor.CROSSWIND && it.status == FlightStatus.NO_GO })
    }
    @Test fun same_wind_aligned_with_runway_has_no_crosswind_finding() {
        val r = FlightEvaluator.evaluate(snap(wind = 35.0, dir = 270.0), UavPlatform.TB2, runwayHeadingDeg = 270)
        assertTrue(r.findings.none { it.factor == RiskFactor.CROSSWIND })
    }
    @Test fun crosswind_not_evaluated_without_runway_and_noted() {
        val r = FlightEvaluator.evaluate(snap(wind = 35.0), UavPlatform.TB2, runwayHeadingDeg = null)
        assertTrue(r.findings.none { it.factor == RiskFactor.CROSSWIND })
        assertTrue(r.notes.any { it.contains("Pist yönü") })
    }
    @Test fun crosswind_uses_gust_not_only_mean_wind() {
        val r = FlightEvaluator.evaluate(snap(wind = 10.0, gust = 40.0, dir = 270.0), UavPlatform.TB2, runwayHeadingDeg = 180)
        assertTrue(r.findings.any { it.factor == RiskFactor.CROSSWIND && it.status == FlightStatus.NO_GO })
    }

    // ---- İrtifa rüzgârı ----
    @Test fun wind_aloft_over_limit_is_no_go() = assertEquals(FlightStatus.NO_GO, status(w = snap(aloft = 70.0)))
    @Test fun wind_aloft_missing_is_noted_not_blocking() {
        val r = FlightEvaluator.evaluate(snap(aloft = null), UavPlatform.TB2)
        assertEquals(FlightStatus.GO, r.status)
        assertTrue(r.notes.any { it.contains("İrtifa rüzgârı") })
    }

    // ---- Görüş ----
    @Test fun low_visibility_is_no_go() = assertEquals(FlightStatus.NO_GO, status(w = snap(vis = 1200.0)))
    @Test fun visibility_near_minimum_is_caution() = assertEquals(FlightStatus.CAUTION, status(w = snap(vis = 2500.0)))
    @Test fun visibility_exactly_minimum_is_not_no_go() = assertEquals(FlightStatus.CAUTION, status(w = snap(vis = 2000.0)))

    // ---- Tavan ----
    @Test fun low_overcast_ceiling_is_no_go() {
        // spread 1 °C => ~125 m < 300 m, örtü %90
        val r = FlightEvaluator.evaluate(snap(temp = 15.0, dew = 14.0, cloudLow = 90.0), UavPlatform.TB2)
        assertTrue(r.findings.any { it.factor == RiskFactor.CEILING && it.status == FlightStatus.NO_GO })
    }
    @Test fun ceiling_ignored_when_low_cloud_cover_is_scattered() {
        val r = FlightEvaluator.evaluate(snap(temp = 15.0, dew = 14.0, cloudLow = 30.0), UavPlatform.TB2)
        assertTrue(r.findings.none { it.factor == RiskFactor.CEILING })
    }

    // ---- Hadiseler ----
    @Test fun thunderstorm_is_no_go() {
        for (code in listOf(95, 96, 99)) assertEquals(FlightStatus.NO_GO, status(w = snap(code = code)))
    }
    @Test fun thunderstorm_is_no_go_for_every_platform() {
        for (p in UavPlatform.entries) assertEquals(FlightStatus.NO_GO, status(p, w = snap(code = 95)))
    }
    @Test fun freezing_rain_and_drizzle_are_no_go() {
        for (code in listOf(56, 57, 66, 67)) assertEquals(FlightStatus.NO_GO, status(w = snap(code = code, temp = 2.0, dew = 1.0)))
    }
    @Test fun heavy_snow_is_no_go() = assertEquals(FlightStatus.NO_GO, status(w = snap(code = 75, temp = 5.0, dew = 2.0)))
    @Test fun fog_is_caution_with_otherwise_good_visibility() {
        val r = FlightEvaluator.evaluate(snap(code = 45, temp = 12.0, dew = 11.0), UavPlatform.TB2)
        assertEquals(FlightStatus.CAUTION, r.status)
    }

    // ---- Yağış ----
    @Test fun light_precipitation_is_caution() = assertEquals(FlightStatus.CAUTION, status(w = snap(precip = 0.5, code = 61)))
    @Test fun heavy_precipitation_is_no_go() = assertEquals(FlightStatus.NO_GO, status(w = snap(precip = 6.0, code = 65)))
    @Test fun precipitation_at_limit_is_not_no_go() = assertEquals(FlightStatus.CAUTION, status(w = snap(precip = 2.0, code = 61)))

    // ---- Sıcaklık / buzlanma ----
    @Test fun dry_sub_zero_is_caution() = assertEquals(FlightStatus.CAUTION, status(w = snap(temp = -3.0, dew = -13.0)))
    @Test fun sub_zero_with_precipitation_is_no_go() =
        assertEquals(FlightStatus.NO_GO, status(w = snap(temp = -2.0, dew = -3.0, precip = 0.4, code = 71)))
    @Test fun above_zero_but_saturated_air_is_icing_caution() {
        val r = FlightEvaluator.evaluate(snap(temp = 3.0, dew = 2.0), UavPlatform.TB2)
        assertEquals(FlightStatus.CAUTION, r.status)
        assertTrue(r.findings.any { it.factor == RiskFactor.THERMAL })
    }
    @Test fun warm_saturated_air_is_not_icing() = assertEquals(FlightStatus.GO, status(w = snap(temp = 15.0, dew = 14.0, cloudLow = 20.0)))
    @Test fun extreme_cold_is_no_go() = assertEquals(FlightStatus.NO_GO, status(w = snap(temp = -25.0, dew = -35.0)))
    @Test fun extreme_heat_is_no_go() = assertEquals(FlightStatus.NO_GO, status(w = snap(temp = 47.0, dew = 5.0)))

    // ---- Yoğunluk irtifası ----
    @Test fun hot_and_high_density_altitude_is_no_go_for_vtol() {
        // 800 hPa ve 40 °C => DA ≈ 3330 m; KALKAN limiti 3000 m
        assertEquals(FlightStatus.NO_GO, status(UavPlatform.KALKAN, w = snap(pressure = 800.0, temp = 40.0, dew = 5.0)))
    }
    @Test fun same_density_altitude_is_go_for_tb2() {
        assertEquals(FlightStatus.GO, status(UavPlatform.TB2, w = snap(pressure = 800.0, temp = 40.0, dew = 5.0)))
    }

    // ---- Bileşik davranış ----
    @Test fun all_findings_are_reported_not_only_the_first() {
        val r = FlightEvaluator.evaluate(snap(wind = 30.0, temp = -3.0, dew = -13.0), UavPlatform.TB2)
        assertEquals(FlightStatus.CAUTION, r.status)
        val factors = r.findings.map { it.factor }
        assertTrue(RiskFactor.WIND in factors && RiskFactor.THERMAL in factors)
    }
    @Test fun worst_status_wins_and_is_listed_first() {
        val r = FlightEvaluator.evaluate(snap(wind = 30.0, code = 95), UavPlatform.TB2)
        assertEquals(FlightStatus.NO_GO, r.status)
        assertEquals(FlightStatus.NO_GO, r.findings.first().status)
        assertTrue(r.findings.size >= 2)
    }
    @Test fun two_cautions_stay_caution() {
        val r = FlightEvaluator.evaluate(snap(wind = 30.0, gust = 30.0, temp = -3.0, dew = -13.0), UavPlatform.TB2)
        // rüzgâr CAUTION + donma altı CAUTION (hamle 30 < 40 => GO)
        assertEquals(FlightStatus.CAUTION, r.status)
    }
    @Test fun three_cautions_escalate_to_no_go_by_risk_accumulation() {
        val r = FlightEvaluator.evaluate(
            snap(wind = 30.0, gust = 42.0, temp = -3.0, dew = -13.0),
            UavPlatform.TB2
        )
        assertEquals(FlightStatus.NO_GO, r.status)
        assertTrue(r.findings.any { it.factor == RiskFactor.RISK_ACCUMULATION })
    }

    // ---- Veri güncelliği ve notlar ----
    @Test fun fresh_data_is_fine() = assertEquals(FlightStatus.GO, status(age = 10, w = snap()))
    @Test fun data_older_than_45_min_is_caution() = assertEquals(FlightStatus.CAUTION, status(age = 46, w = snap()))
    @Test fun data_older_than_90_min_is_no_go() = assertEquals(FlightStatus.NO_GO, status(age = 91, w = snap()))
    @Test fun night_is_noted_but_not_blocking() {
        val r = FlightEvaluator.evaluate(snap(day = false), UavPlatform.TB2)
        assertEquals(FlightStatus.GO, r.status)
        assertTrue(r.notes.any { it.contains("Gece") })
    }

    // ---- Platform tutarlılığı ----
    @Test fun platform_limits_are_internally_consistent() {
        for (p in UavPlatform.entries) {
            assertTrue("${p.name}: hamle ≥ rüzgâr", p.maxGustKmh >= p.maxWindLimitKmh)
            assertTrue("${p.name}: yan rüzgâr ≤ rüzgâr", p.maxCrosswindKmh <= p.maxWindLimitKmh)
            assertTrue("${p.name}: irtifa rüzgârı ≥ yüzey rüzgârı", p.maxWindAloftKmh >= p.maxWindLimitKmh)
        }
    }
}
