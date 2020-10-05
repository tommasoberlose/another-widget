package com.tommasoberlose.anotherwidget.network.repository

import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.tommasoberlose.anotherwidget.network.api.WeatherGovApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherGovRepository {

    private val apiService: WeatherGovApiService = getRetrofit().create(WeatherGovApiService::class.java)

    suspend fun getGridPoints(latitude: String, longitude: String) = apiService.getGridPoints(latitude, longitude)
    suspend fun getWeather(gridId: String, gridX: Double, gridY: Double, unit: String) = apiService.getWeather(gridId, gridX.toInt(), gridY.toInt(), unit)

    companion object {
        private const val BASE_URL = "https://api.weather.gov/"
        private fun getRetrofit(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(NetworkResponseAdapterFactory())
                .build()
        }
    }
}