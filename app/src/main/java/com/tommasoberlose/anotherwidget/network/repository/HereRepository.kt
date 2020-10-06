package com.tommasoberlose.anotherwidget.network.repository

import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.network.api.ApiServices
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HereRepository {

    /* HERE */
    private val apiServiceHere: ApiServices.HereService = getRetrofit().create(ApiServices.HereService::class.java)
    suspend fun getWeather() = apiServiceHere.getWeather(Preferences.weatherProviderApiHere, Preferences.customLocationLat, Preferences.customLocationLon, "observation", true, Preferences.weatherTempUnit != "F")

    companion object {
        private const val BASE_URL_HERE = "https://weather.ls.hereapi.com/weather/1.0/"

        private fun getRetrofit(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL_HERE)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(NetworkResponseAdapterFactory())
                .build()
        }
    }
}