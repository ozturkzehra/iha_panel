package com.example.iha_panel.data

import retrofit2.http.GET
import retrofit2.http.Query

object WeatherVariables {
    const val CURRENT = "temperature_2m,relative_humidity_2m,is_day,precipitation,weather_code," +
        "surface_pressure,visibility,wind_speed_10m,wind_direction_10m,wind_gusts_10m"

    const val HOURLY = "temperature_2m,relative_humidity_2m,dew_point_2m,is_day,precipitation,weather_code," +
        "surface_pressure,visibility,cloud_cover_low,wind_speed_10m,wind_direction_10m,wind_gusts_10m," +
        "wind_speed_80m,wind_speed_120m,wind_speed_180m"

    /** İlk eleman içinde bulunulan saat, ardından +4 saat. */
    const val FORECAST_HOURS = 5
}

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = WeatherVariables.CURRENT,
        @Query("hourly") hourly: String = WeatherVariables.HOURLY,
        @Query("forecast_hours") forecastHours: Int = WeatherVariables.FORECAST_HOURS,
        @Query("wind_speed_unit") windSpeedUnit: String = "kmh",
        @Query("timeformat") timeFormat: String = "unixtime"
    ): WeatherResponse
}
