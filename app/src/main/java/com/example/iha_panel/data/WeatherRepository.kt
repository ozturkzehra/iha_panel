package com.example.iha_panel.data

import com.example.iha_panel.domain.WeatherBundle
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class WeatherRepository {
    private val api: WeatherApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(
            OkHttpClient.Builder()
                .callTimeout(20, TimeUnit.SECONDS)
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApi::class.java)

    /**
     * Şimdiki durum + saatlik tahmin. Başarısızlık [Result.failure] olarak döner;
     * iptal (CancellationException) ise yutulmaz, yeniden fırlatılır.
     */
    suspend fun getWeather(lat: Double, lon: Double): Result<WeatherBundle> =
        try {
            Result.success(api.getForecast(lat, lon).toBundle())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
