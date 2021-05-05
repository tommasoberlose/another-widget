package com.tommasoberlose.anotherwidget.network.repository

import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.tommasoberlose.anotherwidget.network.api.ApiServices
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TimeZonesRepository {

    /* YR */
    private val apiService: ApiServices.TimeZonesService = getRetrofit().create(ApiServices.TimeZonesService::class.java)
    suspend fun getTimeZone(lat: String, long: String) = apiService.getTimeZone(lat, long)

    companion object {
        private const val BASE_URL_YR = "http://api.geonames.org/"

        private fun getRetrofit(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL_YR)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(NetworkResponseAdapterFactory())
                .build()
        }
    }
}