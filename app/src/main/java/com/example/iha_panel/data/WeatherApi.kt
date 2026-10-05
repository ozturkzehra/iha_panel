package com.example.iha_panel.data

import retrofit2.http.GET
import retrofit2.http.Query
import java.nio.DoubleBuffer

//istek atma kodunu retrofit yapacak
interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getCurrent(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current:String = "temperature_2m,wind_speed_10m,precipitation,visibility"
    ): WeatherResponse
}