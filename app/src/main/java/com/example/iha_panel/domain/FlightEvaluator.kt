package com.example.iha_panel.domain

import java.util.Locale

/**
 * Karar eşikleri ve marjlar.
 *
 * Platform limitleri [UavPlatform] içindedir ve DEMO değerlerdir (gerçek değerler uçuş
 * el kitabından gelmelidir). Burada platformdan bağımsız meteorolojik sabitler bulunur.
 * Kural yapısı standart meteoroloji brifingi pratiğini izler: yüzey rüzgârı/hamle, yan
 * rüzgâr, irtifa rüzgârı, görüş, tavan (BKN/OVC), tehlikeli hadiseler, buzlanma,
 * yoğunluk irtifası ve veri güncelliği.
 */
object AviationSafetyStandards {
    const val WIND_CAUTION_BUFFER_KMH = 10.0
    const val GUST_CAUTION_BUFFER_KMH = 10.0
    const val CROSSWIND_CAUTION_BUFFER_KMH = 6.0
    const val WIND_ALOFT_CAUTION_BUFFER_KMH = 15.0
    const val DENSITY_ALTITUDE_CAUTION_BUFFER_M = 500.0

    /** Görüş/tavan için "limite yaklaşma" çarpanı (limit × 1,5 altı = CAUTION). */
    const val VISIBILITY_CAUTION_FACTOR = 1.5
    const val CEILING_CAUTION_FACTOR = 1.5

    /** Tavan tanımı: en düşük BKN/OVC katmanı (≥ 5/8 ≈ %62,5 örtü). */
    const val CEILING_COVER_PCT = 62.5

    const val FREEZING_TEMP_C = 0.0
    const val ICING_MAX_TEMP_C = 5.0
    const val ICING_MIN_TEMP_C = -20.0
    const val ICING_MOISTURE_SPREAD_C = 3.0
    const val COLD_LIMIT_C = -20.0
    const val HEAT_LIMIT_C = 45.0

    const val STALE_CAUTION_MIN = 45L
    const val STALE_NO_GO_MIN = 90L

    /** Bu kadar (veya daha fazla) marjinal etken birleşirse sonuç NO-GO'ya yükselir. */
    const val CAUTION_ACCUMULATION_COUNT = 3
}

enum class RiskFactor(val label: String) {
    DATA_QUALITY("Veri güncelliği"),
    WIND("Yüzey rüzgârı"),
    GUST("Hamle"),
    CROSSWIND("Yan rüzgâr"),
    WIND_ALOFT("İrtifa rüzgârı"),
    VISIBILITY("Görüş"),
    CEILING("Bulut tabanı"),
    PHENOMENON("Hava hadisesi"),
    PRECIPITATION("Yağış"),
    THERMAL("Sıcaklık / Buzlanma"),
    DENSITY_ALTITUDE("Yoğunluk irtifası"),
    RISK_ACCUMULATION("Risk birikimi")
}

data class Finding(
    val factor: RiskFactor,
    val status: FlightStatus,
    val message: String
)

data class EvaluationResult(
    val status: FlightStatus,
    /** En ağır bulgunun mesajı (GO ise genel onay mesajı). */
    val reason: String,
    /** CAUTION/NO-GO üreten TÜM bulgular, en ağırdan hafife. */
    val findings: List<Finding>,
    /** Karar vermeyen bilgi notları (eksik veri, gece operasyonu vb.). */
    val notes: List<String>
)

object FlightEvaluator {

    fun evaluate(
        weather: WeatherSnapshot,
        platform: UavPlatform = UavPlatform.TB2,
        runwayHeadingDeg: Int? = null,
        dataAgeMinutes: Long? = null
    ): EvaluationResult {
        val base = listOfNotNull(
            dataAgeFinding(dataAgeMinutes),
            windFinding(weather, platform),
            gustFinding(weather, platform),
            crosswindFinding(weather, platform, runwayHeadingDeg),
            windAloftFinding(weather, platform),
            visibilityFinding(weather, platform),
            ceilingFinding(weather, platform),
            phenomenonFinding(weather),
            precipitationFinding(weather, platform),
            thermalFinding(weather),
            densityAltitudeFinding(weather, platform)
        )

        val cautionCount = base.count { it.status == FlightStatus.CAUTION }
        val accumulation = if (
            base.none { it.status == FlightStatus.NO_GO } &&
            cautionCount >= AviationSafetyStandards.CAUTION_ACCUMULATION_COUNT
        ) {
            Finding(
                RiskFactor.RISK_ACCUMULATION,
                FlightStatus.NO_GO,
                "Risk birikimi: $cautionCount ayrı marjinal etken aynı anda mevcut"
            )
        } else null

        // sortedByDescending kararlıdır: eşit ağırlıkta kural sırası korunur.
        val findings = (listOfNotNull(accumulation) + base).sortedByDescending { it.status.ordinal }
        val status = findings.firstOrNull()?.status ?: FlightStatus.GO
        val reason = findings.firstOrNull()?.message
            ?: "${platform.platformName} için tüm meteorolojik parametreler operasyonel zarf içinde"

        return EvaluationResult(status, reason, findings, notesFor(weather, runwayHeadingDeg))
    }

    // ---- Kurallar -------------------------------------------------------------------------

    private fun dataAgeFinding(ageMin: Long?): Finding? = when {
        ageMin == null -> null
        ageMin > AviationSafetyStandards.STALE_NO_GO_MIN -> Finding(
            RiskFactor.DATA_QUALITY, FlightStatus.NO_GO,
            "Meteoroloji verisi güncel değil ($ageMin dk önce); yenileyin"
        )
        ageMin > AviationSafetyStandards.STALE_CAUTION_MIN -> Finding(
            RiskFactor.DATA_QUALITY, FlightStatus.CAUTION,
            "Meteoroloji verisi $ageMin dk önceye ait; yenilemeyi düşünün"
        )
        else -> null
    }

    private fun windFinding(w: WeatherSnapshot, p: UavPlatform) = highIsBad(
        RiskFactor.WIND, w.windSpeedKmh, p.maxWindLimitKmh, AviationSafetyStandards.WIND_CAUTION_BUFFER_KMH,
        noGo = "Rüzgâr limiti aşıldı: ${w.windSpeedKmh.n0()} > ${p.maxWindLimitKmh.n0()} km/h (${p.platformName})",
        caution = "Rüzgâr limite yaklaşıyor: ${w.windSpeedKmh.n0()} km/h (limit ${p.maxWindLimitKmh.n0()}), türbülans payı daralıyor"
    )

    private fun gustFinding(w: WeatherSnapshot, p: UavPlatform) = highIsBad(
        RiskFactor.GUST, w.windGustKmh, p.maxGustKmh, AviationSafetyStandards.GUST_CAUTION_BUFFER_KMH,
        noGo = "Hamle limiti aşıldı: ${w.windGustKmh.n0()} > ${p.maxGustKmh.n0()} km/h (${p.platformName})",
        caution = "Hamle limite yaklaşıyor: ${w.windGustKmh.n0()} km/h (limit ${p.maxGustKmh.n0()})"
    )

    private fun crosswindFinding(w: WeatherSnapshot, p: UavPlatform, heading: Int?): Finding? {
        if (heading == null) return null
        // Yan rüzgâr en kötü durum için hamle dahil edilerek hesaplanır.
        val effective = maxOf(w.windSpeedKmh, w.windGustKmh)
        val cross = AviationMath.crosswindKmh(effective, w.windDirectionDeg, heading)
        return highIsBad(
            RiskFactor.CROSSWIND, cross, p.maxCrosswindKmh, AviationSafetyStandards.CROSSWIND_CAUTION_BUFFER_KMH,
            noGo = "Yan rüzgâr limiti aşıldı: ${cross.n0()} > ${p.maxCrosswindKmh.n0()} km/h (pist ${heading}°)",
            caution = "Yan rüzgâr limite yaklaşıyor: ${cross.n0()} km/h (limit ${p.maxCrosswindKmh.n0()}, pist ${heading}°)"
        )
    }

    private fun windAloftFinding(w: WeatherSnapshot, p: UavPlatform): Finding? {
        val aloft = w.windAloftKmh ?: return null
        return highIsBad(
            RiskFactor.WIND_ALOFT, aloft, p.maxWindAloftKmh, AviationSafetyStandards.WIND_ALOFT_CAUTION_BUFFER_KMH,
            noGo = "İrtifa rüzgârı limiti aşıldı: ${aloft.n0()} > ${p.maxWindAloftKmh.n0()} km/h",
            caution = "İrtifa rüzgârı limite yaklaşıyor: ${aloft.n0()} km/h (limit ${p.maxWindAloftKmh.n0()})"
        )
    }

    private fun visibilityFinding(w: WeatherSnapshot, p: UavPlatform) = lowIsBad(
        RiskFactor.VISIBILITY, w.visibilityM, p.minVisibilityMeters, AviationSafetyStandards.VISIBILITY_CAUTION_FACTOR,
        noGo = "Görüş asgari limitin altında: ${w.visibilityM.n0()} < ${p.minVisibilityMeters.n0()} m (${p.platformName})",
        caution = "Görüş limite yakın: ${w.visibilityM.n0()} m (asgari ${p.minVisibilityMeters.n0()} m)"
    )

    private fun ceilingFinding(w: WeatherSnapshot, p: UavPlatform): Finding? {
        val cover = w.cloudCoverLowPct ?: return null
        if (cover < AviationSafetyStandards.CEILING_COVER_PCT) return null
        val ceiling = AviationMath.estimatedCeilingM(w.temperatureC, w.dewPointC)
        return lowIsBad(
            RiskFactor.CEILING, ceiling, p.minCeilingM, AviationSafetyStandards.CEILING_CAUTION_FACTOR,
            noGo = "Bulut tabanı (tahmini) asgari değerin altında: ~${ceiling.n0()} < ${p.minCeilingM.n0()} m",
            caution = "Bulut tabanı (tahmini) alçak: ~${ceiling.n0()} m (asgari ${p.minCeilingM.n0()} m)"
        )
    }

    private fun phenomenonFinding(w: WeatherSnapshot): Finding? = when (w.weatherCode) {
        95, 96, 99 -> Finding(
            RiskFactor.PHENOMENON, FlightStatus.NO_GO,
            "${AviationMath.describeWeatherCode(w.weatherCode)}: şimşek, şiddetli türbülans ve dolu riski"
        )
        56, 57, 66, 67 -> Finding(
            RiskFactor.PHENOMENON, FlightStatus.NO_GO,
            "${AviationMath.describeWeatherCode(w.weatherCode)}: hızlı yapısal buzlanma riski"
        )
        75, 86 -> Finding(
            RiskFactor.PHENOMENON, FlightStatus.NO_GO,
            "${AviationMath.describeWeatherCode(w.weatherCode)}: görüş ve buzlanma riski"
        )
        71, 73, 77, 85 -> Finding(
            RiskFactor.PHENOMENON, FlightStatus.CAUTION,
            "${AviationMath.describeWeatherCode(w.weatherCode)}: görüş düşüşü ve pist/yüzey buzlanmasını izleyin"
        )
        45, 48 -> Finding(
            RiskFactor.PHENOMENON, FlightStatus.CAUTION,
            "${AviationMath.describeWeatherCode(w.weatherCode)}: görüşün hızla düşebileceği koşul"
        )
        else -> null
    }

    private fun precipitationFinding(w: WeatherSnapshot, p: UavPlatform): Finding? = when {
        w.precipitationMm > p.maxPrecipitationMm -> Finding(
            RiskFactor.PRECIPITATION, FlightStatus.NO_GO,
            "Yağış limiti aşıldı: ${w.precipitationMm.n1()} > ${p.maxPrecipitationMm.n1()} mm (${p.platformName})"
        )
        w.precipitationMm > 0.0 -> Finding(
            RiskFactor.PRECIPITATION, FlightStatus.CAUTION,
            "Hafif yağış (${w.precipitationMm.n1()} mm): ıslak pist/frenleme mesafesi ve EO/IR gimbal görüşünü izleyin"
        )
        else -> null
    }

    private fun thermalFinding(w: WeatherSnapshot): Finding? {
        val t = w.temperatureC
        val spread = t - w.dewPointC
        val visibleMoisture = w.precipitationMm > 0.0 || w.weatherCode >= 45
        val anyMoisture = visibleMoisture || spread <= AviationSafetyStandards.ICING_MOISTURE_SPREAD_C
        return when {
            t < AviationSafetyStandards.COLD_LIMIT_C -> Finding(
                RiskFactor.THERMAL, FlightStatus.NO_GO,
                "Aşırı soğuk: ${t.n0()} °C (batarya/yağ/yapı limitleri)"
            )
            t > AviationSafetyStandards.HEAT_LIMIT_C -> Finding(
                RiskFactor.THERMAL, FlightStatus.NO_GO,
                "Aşırı sıcak: ${t.n0()} °C (motor/batarya soğutma limitleri)"
            )
            t in AviationSafetyStandards.ICING_MIN_TEMP_C..AviationSafetyStandards.FREEZING_TEMP_C && visibleMoisture ->
                Finding(
                    RiskFactor.THERMAL, FlightStatus.NO_GO,
                    "Aktif buzlanma koşulu: ${t.n0()} °C ve görünür nem/yağış (pitot, hücum kenarı)"
                )
            t in AviationSafetyStandards.ICING_MIN_TEMP_C..AviationSafetyStandards.ICING_MAX_TEMP_C && anyMoisture ->
                Finding(
                    RiskFactor.THERMAL, FlightStatus.CAUTION,
                    "Buzlanma riski: ${t.n0()} °C, sıcaklık–çiğ noktası farkı ${spread.n1()} °C (yüzeyde soğuma ile donma olabilir)"
                )
            t < AviationSafetyStandards.FREEZING_TEMP_C -> Finding(
                RiskFactor.THERMAL, FlightStatus.CAUTION,
                "Donma altı sıcaklık (${t.n0()} °C): pitot ısıtma ve batarya performansını kontrol edin"
            )
            else -> null
        }
    }

    private fun densityAltitudeFinding(w: WeatherSnapshot, p: UavPlatform): Finding? {
        val da = AviationMath.densityAltitudeM(w.pressureHpa, w.temperatureC)
        return highIsBad(
            RiskFactor.DENSITY_ALTITUDE, da, p.maxDensityAltitudeM, AviationSafetyStandards.DENSITY_ALTITUDE_CAUTION_BUFFER_M,
            noGo = "Yoğunluk irtifası limiti aşıldı: ${da.n0()} > ${p.maxDensityAltitudeM.n0()} m (tırmanma/kalkış performansı)",
            caution = "Yoğunluk irtifası yüksek: ${da.n0()} m (limit ${p.maxDensityAltitudeM.n0()}); kalkış mesafesi uzar"
        )
    }

    private fun notesFor(w: WeatherSnapshot, heading: Int?): List<String> = buildList {
        if (heading == null) add("Pist yönü girilmedi: yan rüzgâr değerlendirilmedi")
        if (w.windAloftKmh == null) add("İrtifa rüzgârı verisi yok: seyir irtifası rüzgârı değerlendirilmedi")
        if (w.cloudCoverLowPct == null) add("Bulut verisi yok: bulut tabanı değerlendirilmedi")
        if (!w.isDay) add("Gece operasyonu: EO sensör kullanılamaz, IR/aydınlatma ve kurtarma planını doğrulayın")
    }

    // ---- Yardımcılar ----------------------------------------------------------------------

    private fun highIsBad(
        factor: RiskFactor, value: Double, limit: Double, buffer: Double,
        noGo: String, caution: String
    ): Finding? = when {
        value > limit -> Finding(factor, FlightStatus.NO_GO, noGo)
        value >= limit - buffer -> Finding(factor, FlightStatus.CAUTION, caution)
        else -> null
    }

    private fun lowIsBad(
        factor: RiskFactor, value: Double, minimum: Double, cautionFactor: Double,
        noGo: String, caution: String
    ): Finding? = when {
        value < minimum -> Finding(factor, FlightStatus.NO_GO, noGo)
        value < minimum * cautionFactor -> Finding(factor, FlightStatus.CAUTION, caution)
        else -> null
    }

    private fun Double.n0(): String = String.format(Locale.ROOT, "%.0f", this)
    private fun Double.n1(): String = String.format(Locale.ROOT, "%.1f", this)
}
