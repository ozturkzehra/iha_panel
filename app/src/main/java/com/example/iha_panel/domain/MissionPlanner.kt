package com.example.iha_panel.domain

enum class MissionPointRole(val label: String) {
    LAUNCH("Kalkış"),
    AREA("Görev Sahası"),
    RECOVERY("İniş / Alternatif")
}

data class PointWeather(
    val role: MissionPointRole,
    val locationName: String,
    val current: WeatherSnapshot,
    val forecast: List<WeatherSnapshot>
)

data class SlotEvaluation(
    val timeEpochSec: Long,
    val result: EvaluationResult
)

data class PointBriefing(
    val role: MissionPointRole,
    val locationName: String,
    val weather: WeatherSnapshot,
    val now: EvaluationResult,
    val outlook: List<SlotEvaluation>
) {
    /** Şimdi + tahmin penceresi boyunca en kötü durum. */
    val worstStatus: FlightStatus
        get() = (listOf(now.status) + outlook.map { it.result.status }).maxBy { it.ordinal }
}

data class MissionBriefing(
    val status: FlightStatus,
    val reason: String,
    /** false ise en az bir görev noktasının verisi alınamadı: karar verilemez (GO sayılmaz). */
    val decisionAvailable: Boolean,
    val missingRoles: Set<MissionPointRole>,
    val points: List<PointBriefing>
)

/**
 * Çok noktalı (kalkış / görev sahası / iniş) ve zaman pencereli (şimdi + sonraki saatler)
 * değerlendirme. Genel karar, tüm nokta ve zaman dilimlerinin en kötüsüdür.
 */
object MissionPlanner {
    const val OUTLOOK_HOURS = 3

    fun brief(
        points: List<PointWeather>,
        missingRoles: Set<MissionPointRole>,
        platform: UavPlatform,
        runwayHeadingDeg: Int?,
        nowEpochSec: Long,
        timeLabel: (Long) -> String = { t -> "+${(t - nowEpochSec + 3599) / 3600} sa" }
    ): MissionBriefing {
        val ordered = points.sortedBy { it.role.ordinal }

        val briefings = ordered.map { p ->
            val ageMin = ((nowEpochSec - p.current.timeEpochSec) / 60).coerceAtLeast(0)
            val now = FlightEvaluator.evaluate(p.current, platform, runwayHeadingDeg, ageMin)
            val outlook = p.forecast
                .filter { it.timeEpochSec > nowEpochSec }
                .sortedBy { it.timeEpochSec }
                .take(OUTLOOK_HOURS)
                .map { SlotEvaluation(it.timeEpochSec, FlightEvaluator.evaluate(it, platform, runwayHeadingDeg, null)) }
            PointBriefing(p.role, p.locationName, p.current, now, outlook)
        }

        // En kötü (nokta, zaman) çifti: eşitlikte önce gelen (önce rol, sonra erken zaman) kazanır.
        var worstStatus = FlightStatus.GO
        var worstReason: String? = null
        for (b in briefings) {
            val candidates = listOf<Pair<String, EvaluationResult>>(b.role.label to b.now) +
                b.outlook.map { "${b.role.label} (${timeLabel(it.timeEpochSec)})" to it.result }
            for ((who, result) in candidates) {
                if (result.status.ordinal > worstStatus.ordinal) {
                    worstStatus = result.status
                    worstReason = "$who: ${result.reason}"
                }
            }
        }

        val available = missingRoles.isEmpty()
        val reason = when {
            !available -> "Veri alınamayan nokta: ${missingRoles.sortedBy { it.ordinal }.joinToString { it.label }}. Karar verilemiyor."
            worstReason != null -> worstReason
            else -> "${platform.platformName} için tüm noktalarda ve önümüzdeki $OUTLOOK_HOURS saatte meteorolojik parametreler zarf içinde"
        }

        return MissionBriefing(worstStatus, reason, available, missingRoles, briefings)
    }
}
