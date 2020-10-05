package com.tommasoberlose.anotherwidget.network.api

import com.haroldadmin.cnradapter.NetworkResponse
import retrofit2.http.*

interface WeatherGovApiService {
    @GET("points/{latitude},{longitude}")
    suspend fun getGridPoints(@Path("latitude") latitude: String, @Path("longitude") longitude: String): NetworkResponse<HashMap<String, Any>, HashMap<String, Any>>

    @Headers("User-Agent: (Another Widget, tommaso.berlose@gmail.com)")
    @GET("gridpoints/{gridId}/{gridX},{gridY}/forecast")
    suspend fun getWeather(@Path("gridId") gridId: String, @Path("gridX") gridX: Int, @Path("gridY") gridY: Int, @Query("units") unit: String): NetworkResponse<HashMap<String, Any>, HashMap<String, Any>>
}