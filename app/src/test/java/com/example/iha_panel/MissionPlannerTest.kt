package com.example.iha_panel

import com.example.iha_panel.domain.FlightStatus
import com.example.iha_panel.domain.MissionPlanner
import com.example.iha_panel.domain.MissionPointRole
import com.example.iha_panel.domain.PointWeather
import com.example.iha_panel.domain.UavPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionPlannerTest {
    private val now = 1_000_000L
    private val hour = 3600L

    private fun point(role: MissionPointRole, current: com.example.iha_panel.domain.WeatherSnapshot, vararg forecast: com.example.iha_panel.domain.WeatherSnapshot) =
        PointWeather(role, role.label, current, forecast.toList())

    private fun brief(vararg p: PointWeather, missing: Set<MissionPointRole> = emptySet()) =
        MissionPlanner.brief(p.toList(), missing, UavPlatform.TB2, null, now) { t -> "T+${(t - now) / hour}" }

    @Test fun all_calm_is_go_with_all_points_listed() {
        val b = brief(
            point(MissionPointRole.LAUNCH, snap(time = now)),
            point(MissionPointRole.AREA, snap(time = now)),
            point(MissionPointRole.RECOVERY, snap(time = now))
        )
        assertEquals(FlightStatus.GO, b.status)
        assertTrue(b.decisionAvailable)
        assertEquals(3, b.points.size)
    }

    @Test fun worst_point_decides_and_is_named() {
        val b = brief(
            point(MissionPointRole.LAUNCH, snap(time = now)),
            point(MissionPointRole.AREA, snap(time = now, code = 95))
        )
        assertEquals(FlightStatus.NO_GO, b.status)
        assertTrue(b.reason.startsWith("Görev Sahası:"))
    }

    @Test fun deteriorating_forecast_turns_go_into_no_go_with_time_label() {
        val b = brief(
            point(
                MissionPointRole.LAUNCH, snap(time = now),
                snap(time = now + hour), snap(time = now + 2 * hour, code = 95), snap(time = now + 3 * hour)
            )
        )
        assertEquals(FlightStatus.NO_GO, b.status)
        assertTrue(b.reason.contains("T+2"))
        assertEquals(FlightStatus.GO, b.points.first().now.status)
    }

    @Test fun only_next_three_future_hours_are_evaluated() {
        val b = brief(
            point(
                MissionPointRole.LAUNCH, snap(time = now),
                snap(time = now - hour, code = 95),        // geçmiş: yok sayılır
                snap(time = now + hour), snap(time = now + 2 * hour), snap(time = now + 3 * hour),
                snap(time = now + 4 * hour, code = 95)    // pencere dışı
            )
        )
        assertEquals(FlightStatus.GO, b.status)
        assertEquals(3, b.points.first().outlook.size)
    }

    @Test fun missing_point_data_means_no_decision_even_if_available_points_are_go() {
        val b = brief(point(MissionPointRole.LAUNCH, snap(time = now)), missing = setOf(MissionPointRole.RECOVERY))
        assertFalse(b.decisionAvailable)
        assertTrue(b.reason.contains("İniş"))
    }

    @Test fun stale_observation_degrades_decision() {
        val b = brief(point(MissionPointRole.LAUNCH, snap(time = now - 100 * 60)))
        assertEquals(FlightStatus.NO_GO, b.status)
    }
}
