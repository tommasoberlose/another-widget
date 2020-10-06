package com.tommasoberlose.anotherwidget.network.repository

import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.network.api.ApiServices
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherbitRepository {

    /* BIT */
    private val apiServiceBit: ApiServices.WeatherBitService = getRetrofit().create(ApiServices.WeatherBitService::class.java)
    suspend fun getWeather() = apiServiceBit.getWeather(Preferences.weatherProviderApiWeatherBit, Preferences.customLocationLat, Preferences.customLocationLon, if (Preferences.weatherTempUnit == "F") "I" else "M")

    companion object {
        private const val BASE_URL_BIT = "https://api.weatherbit.io/v2.0/"

        private fun getRetrofit(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL_BIT)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(NetworkResponseAdapterFactory())
                .build()
        }
    }
}