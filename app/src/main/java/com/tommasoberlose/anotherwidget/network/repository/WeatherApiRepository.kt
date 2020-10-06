package com.tommasoberlose.anotherwidget.network.repository

import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.network.api.ApiServices
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherApiRepository {

    /* WEATHER API*/
    private val apiServiceApi: ApiServices.WeatherApiService = getRetrofit().create(ApiServices.WeatherApiService::class.java)
    suspend fun getWeather() = apiServiceApi.getWeather(Preferences.weatherProviderApiWeatherApi, "${Preferences.customLocationLat},${Preferences.customLocationLon}")

    companion object {
        private const val BASE_URL_API = "http://api.weatherapi.com/v1/"

        private fun getRetrofit(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL_API)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(NetworkResponseAdapterFactory())
                .build()
        }
    }
}