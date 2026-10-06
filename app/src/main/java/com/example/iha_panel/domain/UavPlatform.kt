package com.example.iha_panel.domain

/**
 * Platform adları kamuya açık ürün adlarıdır. Rüzgar, görüş ve yağış limitleri
 * ise demo amaçlı değerlerdir, gerçek operasyonel limitler değildir.
 */
enum class UavPlatform(
    val platformName: String,
    val callsign: String,
    val maxWindLimitKmh: Double,
    val minVisibilityMeters: Double,
    val maxPrecipitationMm: Double,
    val platformType: String
) {
    MINI(
        platformName = "Bayraktar Mini",
        callsign = "MINI-UAV",
        maxWindLimitKmh = 25.0,
        minVisibilityMeters = 3000.0,
        maxPrecipitationMm = 0.5,
        platformType = "Hafif Keşif / El İHA"
    ),
    KALKAN(
        platformName = "Bayraktar KALKAN",
        callsign = "DIHA-VTOL",
        maxWindLimitKmh = 28.0,
        minVisibilityMeters = 2500.0,
        maxPrecipitationMm = 1.0,
        platformType = "Dikey İniş Kalkışlı (VTOL)"
    ),
    TB2(
        platformName = "Bayraktar TB2",
        callsign = "TACTICAL-UAV",
        maxWindLimitKmh = 38.0,
        minVisibilityMeters = 2000.0,
        maxPrecipitationMm = 2.0,
        platformType = "Taktik Silahlı İHA (SİHA)"
    ),
    TB3(
        platformName = "Bayraktar TB3",
        callsign = "NAVAL-UAV",
        maxWindLimitKmh = 42.0,
        minVisibilityMeters = 1800.0,
        maxPrecipitationMm = 3.0,
        platformType = "Deniz Konuşlu / Katlanır Kanat"
    ),
    AKINCI(
        platformName = "Bayraktar AKINCI",
        callsign = "TIHA-MALE",
        maxWindLimitKmh = 50.0,
        minVisibilityMeters = 1200.0,
        maxPrecipitationMm = 4.0,
        platformType = "Taarruzi İHA (Çift Turboprop)"
    ),
    KIZILELMA(
        platformName = "Bayraktar KIZILELMA",
        callsign = "MIUS-JET",
        maxWindLimitKmh = 65.0,
        minVisibilityMeters = 800.0,
        maxPrecipitationMm = 6.0,
        platformType = "Muharip İnsansız Savaş Uçağı"
    )
}