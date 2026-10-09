package com.example.iha_panel.domain

/**
 * DEMO limitleri. Gerçek operasyonel limitler platformun uçuş el kitabından
 * (AFM/POH) ve operatörün onaylı operasyon kılavuzundan alınmalıdır.
 */
enum class UavPlatform(
    val platformName: String,
    val callsign: String,
    val platformType: String,
    /** Ortalama (10 m) rüzgâr limiti, km/h */
    val maxWindLimitKmh: Double,
    /** Hamle limiti, km/h */
    val maxGustKmh: Double,
    /** Kalkış/iniş yan rüzgâr limiti, km/h */
    val maxCrosswindKmh: Double,
    /** Seyir irtifası (80–180 m) rüzgâr limiti, km/h */
    val maxWindAloftKmh: Double,
    val minVisibilityMeters: Double,
    val maxPrecipitationMm: Double,
    /** Asgari bulut tabanı (tavan), m */
    val minCeilingM: Double,
    /** Azami yoğunluk irtifası, m */
    val maxDensityAltitudeM: Double
) {
    MINI(
        platformName = "Bayraktar Mini",
        callsign = "MINI-UAV",
        platformType = "Hafif Keşif / El İHA",
        maxWindLimitKmh = 25.0,
        maxGustKmh = 35.0,
        maxCrosswindKmh = 15.0,
        maxWindAloftKmh = 40.0,
        minVisibilityMeters = 3000.0,
        maxPrecipitationMm = 0.5,
        minCeilingM = 150.0,
        maxDensityAltitudeM = 3500.0
    ),
    KALKAN(
        platformName = "Bayraktar KALKAN",
        callsign = "DIHA-VTOL",
        platformType = "Dikey İniş Kalkışlı (VTOL)",
        maxWindLimitKmh = 28.0,
        maxGustKmh = 38.0,
        maxCrosswindKmh = 18.0,
        maxWindAloftKmh = 45.0,
        minVisibilityMeters = 2500.0,
        maxPrecipitationMm = 1.0,
        minCeilingM = 200.0,
        maxDensityAltitudeM = 3000.0
    ),
    TB2(
        platformName = "Bayraktar TB2",
        callsign = "TACTICAL-UAV",
        platformType = "Taktik Silahlı İHA (SİHA)",
        maxWindLimitKmh = 38.0,
        maxGustKmh = 50.0,
        maxCrosswindKmh = 24.0,
        maxWindAloftKmh = 65.0,
        minVisibilityMeters = 2000.0,
        maxPrecipitationMm = 2.0,
        minCeilingM = 300.0,
        maxDensityAltitudeM = 4500.0
    ),
    TB3(
        platformName = "Bayraktar TB3",
        callsign = "NAVAL-UAV",
        platformType = "Deniz Konuşlu / Katlanır Kanat",
        maxWindLimitKmh = 42.0,
        maxGustKmh = 55.0,
        maxCrosswindKmh = 28.0,
        maxWindAloftKmh = 70.0,
        minVisibilityMeters = 1800.0,
        maxPrecipitationMm = 3.0,
        minCeilingM = 300.0,
        maxDensityAltitudeM = 4500.0
    ),
    AKINCI(
        platformName = "Bayraktar AKINCI",
        callsign = "TIHA-MALE",
        platformType = "Taarruzi İHA (Çift Turboprop)",
        maxWindLimitKmh = 50.0,
        maxGustKmh = 65.0,
        maxCrosswindKmh = 32.0,
        maxWindAloftKmh = 85.0,
        minVisibilityMeters = 1200.0,
        maxPrecipitationMm = 4.0,
        minCeilingM = 450.0,
        maxDensityAltitudeM = 5000.0
    ),
    KIZILELMA(
        platformName = "Bayraktar KIZILELMA",
        callsign = "MIUS-JET",
        platformType = "Muharip İnsansız Savaş Uçağı",
        maxWindLimitKmh = 65.0,
        maxGustKmh = 80.0,
        maxCrosswindKmh = 40.0,
        maxWindAloftKmh = 110.0,
        minVisibilityMeters = 800.0,
        maxPrecipitationMm = 6.0,
        minCeilingM = 300.0,
        maxDensityAltitudeM = 6000.0
    )
}
