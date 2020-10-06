package com.tommasoberlose.anotherwidget.network.repository

import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.tommasoberlose.anotherwidget.network.api.ApiServices
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherGovRepository {

    /* WEATHER GOV*/
    private val apiServiceGov: ApiServices.WeatherGovApiService = getRetrofit().create(ApiServices.WeatherGovApiService::class.java)
    suspend fun getGridPoints(latitude: String, longitude: String) = apiServiceGov.getGridPoints(latitude, longitude)
    suspend fun getWeather(gridId: String, gridX: Double, gridY: Double, unit: String) = apiServiceGov.getWeather(gridId, gridX.toInt(), gridY.toInt(), unit)

    companion object {
        private const val BASE_URL_GOV = "https://api.weather.gov/"

        private fun getRetrofit(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL_GOV)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(NetworkResponseAdapterFactory())
                .build()
        }
    }
}