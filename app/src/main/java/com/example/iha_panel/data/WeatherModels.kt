package com.example.iha_panel.data

import com.google.gson.annotations.SerializedName

/**
 * Open-Meteo yanıtı. Gson, Kotlin null-güvenliğini atladığı için TÜM alanlar nullable'dır;
 * eksik veri [toBundle] içinde açıkça yakalanır (sessizce 0 sayılmaz).
 * Zamanlar `timeformat=unixtime` ile epoch saniyesidir.
 */
data class WeatherResponse(
    @SerializedName("current") val current: CurrentWeather?,
    @SerializedName("hourly") val hourly: HourlyWeather?
)

data class CurrentWeather(
    @SerializedName("time") val time: Long?,
    @SerializedName("temperature_2m") val temperature: Double?,
    @SerializedName("relative_humidity_2m") val humidity: Double?,
    @SerializedName("is_day") val isDay: Int?,
    @SerializedName("precipitation") val precipitation: Double?,
    @SerializedName("weather_code") val weatherCode: Int?,
    @SerializedName("surface_pressure") val pressure: Double?,
    @SerializedName("visibility") val visibility: Double?,
    @SerializedName("wind_speed_10m") val windSpeed: Double?,
    @SerializedName("wind_direction_10m") val windDirection: Double?,
    @SerializedName("wind_gusts_10m") val windGusts: Double?
)

data class HourlyWeather(
    @SerializedName("time") val time: List<Long?>?,
    @SerializedName("temperature_2m") val temperature: List<Double?>?,
    @SerializedName("relative_humidity_2m") val humidity: List<Double?>?,
    @SerializedName("dew_point_2m") val dewPoint: List<Double?>?,
    @SerializedName("is_day") val isDay: List<Int?>?,
    @SerializedName("precipitation") val precipitation: List<Double?>?,
    @SerializedName("weather_code") val weatherCode: List<Int?>?,
    @SerializedName("surface_pressure") val pressure: List<Double?>?,
    @SerializedName("visibility") val visibility: List<Double?>?,
    @SerializedName("cloud_cover_low") val cloudCoverLow: List<Double?>?,
    @SerializedName("wind_speed_10m") val windSpeed: List<Double?>?,
    @SerializedName("wind_direction_10m") val windDirection: List<Double?>?,
    @SerializedName("wind_gusts_10m") val windGusts: List<Double?>?,
    @SerializedName("wind_speed_80m") val windSpeed80: List<Double?>?,
    @SerializedName("wind_speed_120m") val windSpeed120: List<Double?>?,
    @SerializedName("wind_speed_180m") val windSpeed180: List<Double?>?
)
