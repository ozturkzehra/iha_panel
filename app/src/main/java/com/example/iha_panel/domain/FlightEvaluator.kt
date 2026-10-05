package com.example.iha_panel.domain

import com.example.iha_panel.ui.FlightStatus

/**
 * Uluslararası Havacılık Güvenlik Standartları Referansları:
 * - ICAO Annex 2: Minimum Görerek Uçuş Meteorolojik Koşulları (VMC)
 * - NATO STANAG 4671: İHA Sistemleri Uçuşa Elverişlilik ve Rüzgar Emniyet Zarfları
 * - WMO (Dünya Meteoroloji Örgütü): Yağış Şiddeti Eşikleri
 */
object AviationSafetyStandards {
    const val ICING_RISK_TEMP_CELSIUS = 0.0      // Hücum kenarı ve pitot tüpü donma eşiği
    const val MODERATE_TURBULENCE_BUFFER_KMH = 10.0 // Yapısal limite yaklaşıldığında uyarı bandı
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
        val turbulenceThreshold = platform.maxWindLimitKmh - AviationSafetyStandards.MODERATE_TURBULENCE_BUFFER_KMH

        return when {
            // 1. NATO STANAG 4671: Platform Yapısal Rüzgar Limiti Aşımı
            windSpeed > platform.maxWindLimitKmh -> EvaluationResult(
                FlightStatus.NO_GO,
                "STANAG 4671: ${platform.platformName} yapısal rüzgar limiti aşıldı (> ${platform.maxWindLimitKmh.toInt()} km/h)"
            )

            // 2. ICAO Annex 2: Asgari Görüş Eşiği
            visibility < platform.minVisibilityMeters -> EvaluationResult(
                FlightStatus.NO_GO,
                "ICAO Annex 2: ${platform.platformName} için görüş asgari VMC limitinin altında (< ${platform.minVisibilityMeters.toInt()} m)"
            )

            // 3. WMO Standartları: Platform Yağış Toleransı Aşımı
            precipitation > platform.maxPrecipitationMm -> EvaluationResult(
                FlightStatus.NO_GO,
                "WMO Standard: ${platform.platformName} şiddetli yağış limiti aşıldı (> ${platform.maxPrecipitationMm} mm)"
            )

            // 4. Türbülans ve Sınır Değer Riski
            windSpeed in turbulenceThreshold..platform.maxWindLimitKmh -> EvaluationResult(
                FlightStatus.CAUTION,
                "STANAG 4671: ${platform.callsign} için limit yaklaşma türbülansı (${turbulenceThreshold.toInt()}–${platform.maxWindLimitKmh.toInt()} km/h)"
            )

            // 5. Islak Pist ve Optik Sensör Uyarısı
            precipitation > 0.0 -> EvaluationResult(
                FlightStatus.CAUTION,
                "Hafif yağış: İniş takımı frenleme mesafesini ve EO/IR gimbal görüşünü izleyin"
            )

            // 6. Yapısal Buzlanma Tehlikesi
            temperature < AviationSafetyStandards.ICING_RISK_TEMP_CELSIUS -> EvaluationResult(
                FlightStatus.CAUTION,
                "Buzlanma riski: Pitot tüpü ve aerodinamik yüzey donma uyarısı (< 0°C)"
            )

            // 7. Nominal Uçuş Zarfı
            else -> EvaluationResult(
                FlightStatus.GO,
                "${platform.platformName} için tüm meteorolojik parametreler operasyonel zarf içinde"
            )
        }
    }
}