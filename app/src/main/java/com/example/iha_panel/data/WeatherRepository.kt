package com.example.iha_panel.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository {
    private val api = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApi::class.java)

    suspend fun getWeather(lat: Double, lon: Double): Result<CurrentWeather> =
        runCatching { api.getCurrent(lat, lon).current }
}