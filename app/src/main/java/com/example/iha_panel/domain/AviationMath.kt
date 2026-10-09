package com.example.iha_panel.domain

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

/** Brifingde kullanılan standart atmosfer / meteoroloji hesapları. */
object AviationMath {

    /** Yan rüzgâr bileşeni. [windFromDeg] rüzgârın geldiği yön, [runwayHeadingDeg] pist yönü. */
    fun crosswindKmh(windKmh: Double, windFromDeg: Double, runwayHeadingDeg: Int): Double {
        val angle = Math.toRadians(windFromDeg - runwayHeadingDeg)
        return abs(windKmh * sin(angle))
    }

    /** Magnus yaklaşımı ile çiğ noktası (°C). Bağıl nem 0 veya negatifse null. */
    fun dewPointC(tempC: Double, relativeHumidityPct: Double): Double? {
        if (relativeHumidityPct <= 0.0) return null
        val b = 17.625
        val c = 243.04
        val gamma = ln(relativeHumidityPct / 100.0) + b * tempC / (c + tempC)
        return c * gamma / (b - gamma)
    }

    /** İstasyon basıncından (hPa) basınç irtifası (m), ISA'ya göre. */
    fun pressureAltitudeM(stationPressureHpa: Double): Double =
        44330.77 * (1.0 - (stationPressureHpa / 1013.25).pow(0.190263))

    /** Yoğunluk irtifası (m): PA + ~36,6 m/°C × (OAT − ISA sıcaklığı). */
    fun densityAltitudeM(stationPressureHpa: Double, tempC: Double): Double {
        val pa = pressureAltitudeM(stationPressureHpa)
        val isaTemp = 15.0 - 0.0065 * pa
        return pa + 36.576 * (tempC - isaTemp)
    }

    /**
     * Bulut tabanı tahmini (m): (T − Td) × 125. Yalnızca düşük bulut örtüsü varken anlamlıdır.
     * Gerçek ölçüm (ceilometer/METAR) yerine geçmez.
     */
    fun estimatedCeilingM(tempC: Double, dewPointC: Double): Double =
        ((tempC - dewPointC).coerceAtLeast(0.0)) * 125.0

    /** WMO hava durumu kodu (Open-Meteo weather_code) için Türkçe açıklama. */
    fun describeWeatherCode(code: Int): String = when (code) {
        0 -> "Açık"
        1 -> "Çoğunlukla açık"
        2 -> "Parçalı bulutlu"
        3 -> "Kapalı"
        45 -> "Sis"
        48 -> "Kırağılı sis"
        51, 53, 55 -> "Çiseleme"
        56, 57 -> "Dondurucu çiseleme"
        61 -> "Hafif yağmur"
        63 -> "Orta şiddetli yağmur"
        65 -> "Şiddetli yağmur"
        66, 67 -> "Dondurucu yağmur"
        71 -> "Hafif kar"
        73 -> "Orta şiddetli kar"
        75 -> "Yoğun kar"
        77 -> "Kar taneleri"
        80 -> "Hafif sağanak"
        81 -> "Orta şiddetli sağanak"
        82 -> "Şiddetli sağanak"
        85 -> "Hafif kar sağanağı"
        86 -> "Yoğun kar sağanağı"
        95 -> "Gök gürültülü fırtına"
        96, 99 -> "Dolulu fırtına"
        else -> "Kod $code"
    }
}
