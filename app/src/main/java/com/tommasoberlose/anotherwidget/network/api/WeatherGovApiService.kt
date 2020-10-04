package com.tommasoberlose.anotherwidget.network.api

import retrofit2.http.GET
import retrofit2.http.Path

interface WeatherGovApiService {
    @GET("points/{latitude},{longitude}")
    suspend fun getGridPoints(@Path("latitude") latitude: String, @Path("longitude") longitude: String): HashMap<String, Any>

    @GET("gridpoints/{gridId}/{gridX},{gridY}/forecast")
    suspend fun getWeather(@Path("gridId") gridId: String, @Path("gridX") gridX: Int, @Path("gridY") gridY: Int): HashMap<String, Any>
}