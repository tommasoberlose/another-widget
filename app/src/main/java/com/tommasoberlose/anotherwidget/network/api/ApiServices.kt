package com.tommasoberlose.anotherwidget.network.api

import com.haroldadmin.cnradapter.NetworkResponse
import retrofit2.http.*

object ApiServices {
    interface WeatherGovApiService {
        @Headers("User-Agent: (Another Widget, tommaso.berlose@gmail.com)")
        @GET("points/{latitude},{longitude}")
        suspend fun getGridPoints(
            @Path("latitude") latitude: String,
            @Path("longitude") longitude: String
        ): NetworkResponse<HashMap<String, Any>, HashMap<String, Any>>

        @Headers("User-Agent: (Another Widget, tommaso.berlose@gmail.com)")
        @GET("gridpoints/{gridId}/{gridX},{gridY}/forecast")
        suspend fun getWeather(
            @Path("gridId") gridId: String,
            @Path("gridX") gridX: Int,
            @Path("gridY") gridY: Int,
            @Query("units") unit: String
        ): NetworkResponse<HashMap<String, Any>, HashMap<String, Any>>
    }

    interface WeatherBitService {
        @GET("current")
        suspend fun getWeather(
            @Query("key") key: String,
            @Query("lat") lat: String,
            @Query("lon") lon: String,
            @Query("units") units: String,
        ): NetworkResponse<HashMap<String, Any>, HashMap<String, Any>>
    }

    interface WeatherApiService {
        @Headers("Accept: application/json")
        @GET("current.json")
        suspend fun getWeather(
            @Query("key") key: String,
            @Query("q") location: String,
        ): NetworkResponse<HashMap<String, Any>, HashMap<String, Any>>
    }

    interface HereService {
        @GET("report.json")
        suspend fun getWeather(
            @Query("apiKey") apiKey: String,
            @Query("latitude") latitude: String,
            @Query("longitude") longitude: String,
            @Query("product") product: String,
            @Query("oneobservation") oneobservation: Boolean,
            @Query("metric") metric: Boolean,
        ): NetworkResponse<HashMap<String, Any>, HashMap<String, Any>>
    }

    interface YrService {
        @Headers("User-Agent: AnotherWidget")
        @GET("compact.json")
        suspend fun getWeather(
            @Query("lat") lat: String,
            @Query("lon") lon: String,
        ): NetworkResponse<HashMap<String, Any>, HashMap<String, Any>>
    }

    interface TimeZonesService {
        @GET("timezoneJSON")
        suspend fun getTimeZone(
            @Query("lat") lat: String,
            @Query("lng") lon: String,
            @Query("username") username: String = "tommaso.berlose",
        ): NetworkResponse<HashMap<String, Any>, HashMap<String, Any>>
    }
}