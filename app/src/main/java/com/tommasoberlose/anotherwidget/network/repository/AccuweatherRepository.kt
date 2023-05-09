package com.tommasoberlose.anotherwidget.network.repository

import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.network.api.ApiServices
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AccuweatherRepository {

    /* ACCUWEATHER */
    private val apiServiceAccu: ApiServices.AccuweatherService = getRetrofit().create(ApiServices.AccuweatherService::class.java)
    suspend fun getLocation() = apiServiceAccu.getLocation(Preferences.weatherProviderApiAccuweather, "${Preferences.customLocationLat},${Preferences.customLocationLon}")
    suspend fun getWeather(locationKey: String) = apiServiceAccu.getWeather(locationKey, Preferences.weatherProviderApiAccuweather)

    companion object {
        private const val BASE_URL_ACCU = "https://dataservice.accuweather.com/"

        private fun getRetrofit(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL_ACCU)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(NetworkResponseAdapterFactory())
                .build()
        }
    }

    fun getWeatherIcon(icon: Int, isDaytime: Boolean): String = when(icon) {
        1, 2, 30, 33, 34 -> "01"
        3, 4, 35, 36 -> "02"
        5, 37 -> "50"
        6, 38 -> "03"
        7, 8 -> "04"
        11 -> "82"
        12, 13, 14, 18, 39, 40 -> "10"
        15 -> "09"
        16, 17, 41, 42 -> "11"
        32 -> "80"
        19, 20, 21, 22, 23, 24, 31, 43, 44 -> "13"
        25, 26, 29 -> "81"
        else -> ""
    } + if (isDaytime) "d" else "n"
}
