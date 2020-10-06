package com.tommasoberlose.anotherwidget.network.repository

import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.network.api.ApiServices
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class YrRepository {

    /* YR */
    private val apiServiceYr: ApiServices.YrService = getRetrofit().create(ApiServices.YrService::class.java)
    suspend fun getWeather() = apiServiceYr.getWeather(Preferences.customLocationLat, Preferences.customLocationLon)

    companion object {
        private const val BASE_URL_YR = "https://api.met.no/weatherapi/locationforecast/2.0/"

        private fun getRetrofit(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL_YR)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(NetworkResponseAdapterFactory())
                .build()
        }
    }
}