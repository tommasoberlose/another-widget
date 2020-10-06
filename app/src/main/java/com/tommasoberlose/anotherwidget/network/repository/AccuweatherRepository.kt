package com.tommasoberlose.anotherwidget.network.repository

import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.tommasoberlose.anotherwidget.network.api.ApiServices
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AccuweatherRepository {

    /* ACCUWEATHER */
    private val apiServiceAccu: ApiServices.AccuweatherService = getRetrofit().create(ApiServices.AccuweatherService::class.java)
    suspend fun getWeather(): Nothing = TODO()

    companion object {
        private const val BASE_URL_ACCU = ""

        private fun getRetrofit(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL_ACCU)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(NetworkResponseAdapterFactory())
                .build()
        }
    }
}