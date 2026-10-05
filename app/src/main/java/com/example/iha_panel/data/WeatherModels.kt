package com.example.iha_panel.data

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("current")
    val current: CurrentWeather
)
data class CurrentWeather(
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("wind_speed_10m") val windSpeed:Double,
    @SerializedName("precipitation") val precipitation:Double,
    @SerializedName("visibility") val visibility: Double
)