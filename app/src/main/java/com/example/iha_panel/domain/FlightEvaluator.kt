package com.example.iha_panel.domain

import com.example.iha_panel.ui.FlightStatus

/**
 * DEMO eşik değerleri.
 *
 * Bu projedeki rüzgar, görüş ve yağış limitleri eğitim amaçlı, kendi belirlediğim
 * değerlerdir. Gerçek bir platformun operasyonel limitleri ya da herhangi bir
 * standardın (STANAG, ICAO, WMO vb.) kuralları DEĞİLDİR.
 */
object DemoThresholds {
    // Basit buzlanma sezgisi: 0 °C altında uyarı ver
    const val ICING_RISK_TEMP_CELSIUS = 0.0

    // Rüzgar limitine bu kadar yaklaşınca CAUTION ver
    const val WIND_CAUTION_BUFFER_KMH = 10.0
}

data class EvaluationResult(
    val status: FlightStatus,
    val reason: String
)

object FlightEvaluator {

    fun evaluate(
        windSpeed: Double,
        visibility: Double,
        precipitation: Double,
        temperature: Double,
        platform: UavPlatform = UavPlatform.TB2
    ): EvaluationResult {
        val windCautionStart = platform.maxWindLimitKmh - DemoThresholds.WIND_CAUTION_BUFFER_KMH

        return when {
            // 1. Rüzgar demo limiti aşıldı
            windSpeed > platform.maxWindLimitKmh -> EvaluationResult(
                FlightStatus.NO_GO,
                "Rüzgar demo limiti aşıldı (> ${platform.maxWindLimitKmh.toInt()} km/h)"
            )

            // 2. Görüş demo asgari değerinin altında
            visibility < platform.minVisibilityMeters -> EvaluationResult(
                FlightStatus.NO_GO,
                "Görüş demo asgari değerin altında (< ${platform.minVisibilityMeters.toInt()} m)"
            )

            // 3. Yağış demo limiti aşıldı
            precipitation > platform.maxPrecipitationMm -> EvaluationResult(
                FlightStatus.NO_GO,
                "Yağış demo limiti aşıldı (> ${platform.maxPrecipitationMm} mm)"
            )

            // 4. Rüzgar limite yaklaşıyor
            windSpeed >= windCautionStart -> EvaluationResult(
                FlightStatus.CAUTION,
                "Rüzgar demo limite yaklaşıyor (${windCautionStart.toInt()}–${platform.maxWindLimitKmh.toInt()} km/h)"
            )

            // 5. Hafif yağış
            precipitation > 0.0 -> EvaluationResult(
                FlightStatus.CAUTION,
                "Hafif yağış: pist ve sensör görüşünü izleyin"
            )

            // 6. Buzlanma sezgisi
            temperature < DemoThresholds.ICING_RISK_TEMP_CELSIUS -> EvaluationResult(
                FlightStatus.CAUTION,
                "Buzlanma riski: sıcaklık 0 °C altında"
            )

            // 7. Her şey demo limitlerin içinde
            else -> EvaluationResult(
                FlightStatus.GO,
                "Ölçülen değerler demo limitlerin içinde"
            )
        }
    }
}