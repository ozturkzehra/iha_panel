package com.example.iha_panel.domain

data class MissionLocation(
    val name: String,
    val lat: Double,
    val lon: Double
) {
    /** Aynı koordinat için tekrar istek atmamak adına kullanılır. */
    val key: String get() = "%.3f,%.3f".format(java.util.Locale.ROOT, lat, lon)
}

object MissionLocations {
    val ISTANBUL = MissionLocation("İstanbul", 41.0082, 28.9784)

    val presets: List<MissionLocation> = listOf(
        ISTANBUL,
        MissionLocation("Çorlu", 41.1590, 27.8000),
        MissionLocation("Ankara", 39.9334, 32.8597),
        MissionLocation("İzmir", 38.4237, 27.1428),
        MissionLocation("Antalya", 36.8969, 30.7133),
        MissionLocation("Sinop", 42.0231, 35.1531),
        MissionLocation("Trabzon", 41.0027, 39.7168),
        MissionLocation("Erzurum", 39.9043, 41.2679),
        MissionLocation("Diyarbakır", 37.9144, 40.2306),
        MissionLocation("Batman", 37.8812, 41.1351),
        MissionLocation("Van", 38.4891, 43.4089)
    )
}
