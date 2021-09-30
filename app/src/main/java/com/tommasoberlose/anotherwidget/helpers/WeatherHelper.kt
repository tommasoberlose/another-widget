package com.tommasoberlose.anotherwidget.helpers

import android.Manifest
import android.content.Context
import com.chibatching.kotpref.Kotpref
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.network.WeatherNetworkApi
import com.tommasoberlose.anotherwidget.services.LocationService
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.isDarkTheme


/**
 * Created by tommaso on 08/10/17.
 */

object WeatherHelper {

    suspend fun updateWeather(context: Context) {
        Kotpref.init(context)
        if (Preferences.customLocationAdd != "") {
            WeatherNetworkApi(context).updateWeather()
        } else if (context.checkGrantedPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            LocationService.requestNewLocation(context)
        } else {
            Preferences.weatherProviderLocationError = context.getString(R.string.weather_provider_error_missing_location)
            Preferences.weatherProviderError = ""
            removeWeather(context)
            org.greenrobot.eventbus.EventBus.getDefault().post(
                com.tommasoberlose.anotherwidget.ui.fragments.MainFragment.UpdateUiMessageEvent()
            )
        }
    }

    fun removeWeather(context: Context) {
        Preferences.remove(Preferences::weatherTemp)
        Preferences.remove(Preferences::weatherRealTempUnit)
        Preferences.remove(Preferences::weatherIcon)
        MainWidget.updateWidget(context)
    }

    fun getProviderName(context: Context, provider: Constants.WeatherProvider = Constants.WeatherProvider.fromInt(Preferences.weatherProvider)!!): String {
        return context.getString(when(provider) {
            Constants.WeatherProvider.OPEN_WEATHER -> R.string.settings_weather_provider_open_weather
            Constants.WeatherProvider.WEATHER_BIT -> R.string.settings_weather_provider_weatherbit
            Constants.WeatherProvider.WEATHER_API -> R.string.settings_weather_provider_weather_api
            Constants.WeatherProvider.HERE -> R.string.settings_weather_provider_here
            Constants.WeatherProvider.ACCUWEATHER -> R.string.settings_weather_provider_accuweather
            Constants.WeatherProvider.WEATHER_GOV -> R.string.settings_weather_provider_weather_gov
            Constants.WeatherProvider.YR -> R.string.settings_weather_provider_yr
        })
    }

    fun getProviderInfoTitle(context: Context, provider: Constants.WeatherProvider? = Constants.WeatherProvider.fromInt(Preferences.weatherProvider)!!): String {
        return context.getString(when(provider) {
            Constants.WeatherProvider.OPEN_WEATHER -> R.string.weather_provider_info_open_weather_title
            Constants.WeatherProvider.WEATHER_BIT -> R.string.weather_provider_info_weatherbit_title
            Constants.WeatherProvider.WEATHER_API -> R.string.weather_provider_info_weatherapi_title
            Constants.WeatherProvider.HERE -> R.string.weather_provider_info_here_title
            Constants.WeatherProvider.ACCUWEATHER -> R.string.weather_provider_info_accuweather_title
            Constants.WeatherProvider.WEATHER_GOV -> R.string.weather_provider_info_weather_gov_title
            Constants.WeatherProvider.YR -> R.string.weather_provider_info_yr_title
            else -> R.string.nothing
        })
    }

    fun getProviderInfoSubtitle(context: Context, provider: Constants.WeatherProvider? = Constants.WeatherProvider.fromInt(Preferences.weatherProvider)!!): String {
        return context.getString(when(provider) {
            Constants.WeatherProvider.OPEN_WEATHER -> R.string.weather_provider_info_open_weather_subtitle
            Constants.WeatherProvider.WEATHER_BIT -> R.string.weather_provider_info_weatherbit_subtitle
            Constants.WeatherProvider.WEATHER_API -> R.string.weather_provider_info_weatherapi_subtitle
            Constants.WeatherProvider.HERE -> R.string.weather_provider_info_here_subtitle
            Constants.WeatherProvider.ACCUWEATHER -> R.string.weather_provider_info_accuweather_subtitle
            Constants.WeatherProvider.WEATHER_GOV -> R.string.weather_provider_info_weather_gov_subtitle
            Constants.WeatherProvider.YR -> R.string.weather_provider_info_yr_subtitle
            else -> R.string.nothing
        })
    }

    fun getProviderLink(provider: Constants.WeatherProvider? = Constants.WeatherProvider.fromInt(Preferences.weatherProvider)!!): String {
        return when(provider) {
            Constants.WeatherProvider.OPEN_WEATHER -> "https://home.openweathermap.org/users/sign_in"
            Constants.WeatherProvider.WEATHER_BIT -> "https://www.weatherbit.io/account/login"
            Constants.WeatherProvider.WEATHER_API -> "https://www.weatherapi.com/login.aspx"
            Constants.WeatherProvider.HERE -> "https://developer.here.com/login"
            Constants.WeatherProvider.ACCUWEATHER -> "https://developer.accuweather.com/user/login"
            Constants.WeatherProvider.WEATHER_GOV -> "http://www.weather.gov/"
            Constants.WeatherProvider.YR -> "https://www.yr.no/"
            else -> ""
        }
    }

    fun isKeyRequired(provider: Constants.WeatherProvider? = Constants.WeatherProvider.fromInt(Preferences.weatherProvider)!!): Boolean = when (provider) {
        Constants.WeatherProvider.OPEN_WEATHER,
        Constants.WeatherProvider.WEATHER_BIT,
        Constants.WeatherProvider.WEATHER_API,
        Constants.WeatherProvider.HERE,
        Constants.WeatherProvider.ACCUWEATHER -> true

        Constants.WeatherProvider.WEATHER_GOV,
        Constants.WeatherProvider.YR -> false
        else -> true
    }

    fun getApiKey(provider: Constants.WeatherProvider? = Constants.WeatherProvider.fromInt(Preferences.weatherProvider)!!): String = when (provider) {
        Constants.WeatherProvider.OPEN_WEATHER -> Preferences.weatherProviderApiOpen
        Constants.WeatherProvider.WEATHER_BIT -> Preferences.weatherProviderApiWeatherBit
        Constants.WeatherProvider.WEATHER_API -> Preferences.weatherProviderApiWeatherApi
        Constants.WeatherProvider.HERE -> Preferences.weatherProviderApiHere
        Constants.WeatherProvider.ACCUWEATHER -> Preferences.weatherProviderApiAccuweather
        Constants.WeatherProvider.WEATHER_GOV -> ""
        Constants.WeatherProvider.YR -> ""
        else -> ""
    }

    fun getWeatherIconResource(context: Context, icon: String, style: Int = Preferences.weatherIconPack): Int {
        return when (icon) {
            "01d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.clear_day_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.clear_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.clear_day_4
                    else -> if (context.isDarkTheme()) R.drawable.clear_day_5 else R.drawable.clear_day_5_light
                }
            }
            "02d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.partly_cloudy_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.partly_cloudy_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.partly_cloudy_4
                    else -> if (context.isDarkTheme()) R.drawable.partly_cloudy_5 else R.drawable.partly_cloudy_5_light
                }
            }
            "03d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.mostly_cloudy_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.mostly_cloudy_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.mostly_cloudy_4
                    else -> if (context.isDarkTheme()) R.drawable.mostly_cloudy_5 else R.drawable.mostly_cloudy_5_light
                }
            }
            "04d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.cloudy_weather_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.cloudy_weather_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.cloudy_weather_4
                    else -> if (context.isDarkTheme()) R.drawable.cloudy_weather_5 else R.drawable.cloudy_weather_5_light
                }
            }
            "09d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.storm_weather_day_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.storm_weather_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.storm_weather_day_4
                    else -> if (context.isDarkTheme()) R.drawable.storm_weather_day_5 else R.drawable.storm_weather_day_5_light
                }
            }
            "10d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.rainy_day_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.rainy_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.rainy_day_4
                    else -> if (context.isDarkTheme()) R.drawable.rainy_day_5 else R.drawable.rainy_day_5_light
                }
            }
            "11d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.thunder_day_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.thunder_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.thunder_day_4
                    else -> if (context.isDarkTheme()) R.drawable.thunder_day_5 else R.drawable.thunder_day_5_light
                }
            }
            "13d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.snow_day_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.snow_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.snow_day_4
                    else -> if (context.isDarkTheme()) R.drawable.snow_day_5 else R.drawable.snow_day_5_light
                }
            }
            "50d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.haze_day_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.haze_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.haze_day_4
                    else -> if (context.isDarkTheme()) R.drawable.haze_day_5 else R.drawable.haze_day_5_light
                }
            }
            "80d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.windy_day_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.windy_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.windy_day_4
                    else -> if (context.isDarkTheme()) R.drawable.windy_day_5 else R.drawable.windy_day_5_light
                }
            }
            "81d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.rain_snow_day_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.rain_snow_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.rain_snow_day_4
                    else -> if (context.isDarkTheme()) R.drawable.rain_snow_day_5 else R.drawable.rain_snow_day_5_light
                }
            }
            "82d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.haze_weather_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.haze_weather_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.haze_weather_4
                    else -> if (context.isDarkTheme()) R.drawable.haze_weather_5 else R.drawable.haze_weather_5_light
                }
            }



            "01n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.clear_night_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.clear_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.clear_night_4
                    else -> if (context.isDarkTheme()) R.drawable.clear_night_5 else R.drawable.clear_night_5_light
                }
            }
            "02n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.partly_cloudy_night_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.partly_cloudy_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.partly_cloudy_night_4
                    else -> if (context.isDarkTheme()) R.drawable.partly_cloudy_night_5 else R.drawable.partly_cloudy_night_5_light
                }
            }
            "03n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.mostly_cloudy_night_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.mostly_cloudy_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.mostly_cloudy_night_4
                    else -> if (context.isDarkTheme()) R.drawable.mostly_cloudy_night_5 else R.drawable.mostly_cloudy_night_5_light
                }
            }
            "04n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.cloudy_weather_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.cloudy_weather_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.cloudy_weather_4
                    else -> if (context.isDarkTheme()) R.drawable.cloudy_weather_5 else R.drawable.cloudy_weather_5_light
                }
            }
            "09n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.storm_weather_night_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.storm_weather_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.storm_weather_night_4
                    else -> if (context.isDarkTheme()) R.drawable.storm_weather_night_5 else R.drawable.storm_weather_night_5_light
                }
            }
            "10n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.rainy_night_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.rainy_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.rainy_night_4
                    else -> if (context.isDarkTheme()) R.drawable.rainy_night_5 else R.drawable.rainy_night_5_light
                }
            }
            "11n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.thunder_night_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.thunder_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.thunder_night_4
                    else -> if (context.isDarkTheme()) R.drawable.thunder_night_5 else R.drawable.thunder_night_5_light
                }
            }
            "13n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.snow_night_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.snow_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.snow_night_4
                    else -> if (context.isDarkTheme()) R.drawable.snow_night_5 else R.drawable.snow_night_5_light
                }
            }
            "50n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.haze_night_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.haze_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.haze_night_4
                    else -> if (context.isDarkTheme()) R.drawable.haze_night_5 else R.drawable.haze_night_5_light
                }
            }
            "80n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.windy_night_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.windy_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.windy_night_4
                    else -> if (context.isDarkTheme()) R.drawable.windy_night_5 else R.drawable.windy_night_5_light
                }
            }
            "81n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.rain_snow_night_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.rain_snow_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.rain_snow_night_4
                    else -> if (context.isDarkTheme()) R.drawable.rain_snow_night_5 else R.drawable.rain_snow_night_5_light
                }
            }
            "82n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.rawValue -> R.drawable.haze_weather_3
                    Constants.WeatherIconPack.MINIMAL.rawValue -> R.drawable.haze_weather_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.rawValue -> R.drawable.haze_weather_4
                    else -> if (context.isDarkTheme()) R.drawable.haze_weather_5 else R.drawable.haze_weather_5_light
                }
            }
            else -> {
                return if (context.isDarkTheme()) R.drawable.unknown_dark else R.drawable.unknown_light
            }
        }
    }

    fun getWeatherLabel(context: Context, icon: String): String {
        return when (icon) {
            "01d", "01n" -> context.getString(R.string.weather_label_clear)
            "02d", "02n" -> context.getString(R.string.weather_label_partly_cloudy)
            "03d", "03n" -> context.getString(R.string.weather_label_mostly_cloudy)
            "04d", "04n" -> context.getString(R.string.weather_label_cloudy_weather)
            "09d", "09n" -> context.getString(R.string.weather_label_storm_weather)
            "10d", "10n" -> context.getString(R.string.weather_label_rainy)
            "11d", "11n" -> context.getString(R.string.weather_label_thunder)
            "13d", "13n" -> context.getString(R.string.weather_label_snow)
            "50d", "50n", "82d", "82n" -> context.getString(R.string.weather_label_haze)
            "80d", "80n" -> context.getString(R.string.weather_label_windy)
            "81d", "81n" -> context.getString(R.string.weather_label_rain_snow)
            else -> context.getString(R.string.weather_label_unknown)
        }
    }

    fun getWeatherGovIcon(iconString: String, isDaytime: Boolean): String = when (iconString.substringBefore('?').substringAfterLast('/')) {
        "skc" -> "01"
        "few" -> "02"
        "sct" -> "02"
        "bkn" -> "03"
        "ovc" -> "04"
        "wind_skc" -> "01"
        "wind_few" -> "02"
        "wind_sct" -> "02"
        "wind_bkn" -> "03"
        "wind_ovc" -> "04"
        "snow" -> "13"
        "rain_snow" -> "81"
        "rain_sleet" -> "81"
        "snow_sleet" -> "81"
        "fzra" -> "81"
        "rain_fzra" -> "81"
        "snow_fzra" -> "81"
        "sleet" -> "81"
        "rain" -> "10"
        "rain_showers" -> "10"
        "rain_showers_hi" -> "10"
        "tsra" -> "09"
        "tsra_sct" -> "11"
        "tsra_hi" -> "11"
        "tornado" -> "80"
        "hurricane" -> "80"
        "tropical_storm" -> "09"
        "dust" -> "50"
        "smoke" -> "50"
        "haze" -> "50"
        "hot" -> "01"
        "cold" -> "13"
        "blizzard" -> "13"
        "fog" -> "82"
        else -> ""
    } + if (isDaytime) "d" else "n"

    fun getWeatherBitIcon(iconString: String): String = when (iconString.substring(0, 3)) {
        "t01" -> "11"
        "t02" -> "11"
        "t03" -> "09"
        "t04" -> "11"
        "t05" -> "11"
        "d01" -> "10"
        "d02" -> "10"
        "d03" -> "10"
        "r01" -> "10"
        "r02" -> "10"
        "r03" -> "10"
        "f01" -> "10"
        "r04" -> "10"
        "r05" -> "10"
        "r06" -> "10"
        "s01" -> "13"
        "s02" -> "13"
        "s03" -> "13"
        "s04" -> "81"
        "s05" -> "81"
        "s06" -> "13"
        "a01" -> "50"
        "a02" -> "50"
        "a03" -> "50"
        "a04" -> "50"
        "a05" -> "82"
        "a06" -> "82"
        "c01" -> "01"
        "c02" -> "02"
        "c03" -> "03"
        "c04" -> "04"
        else -> ""
    } + iconString.substring(3)

    fun getWeatherApiIcon(icon: Int, isDaytime: Boolean): String = when(icon) {
        1000 -> "01"
        1003 -> "02"
        1006 -> "03"
        1009 -> "04"
        1030 -> "50"
        1063 -> "10"
        1066 -> "13"
        1069 -> "81"
        1072 -> "81"
        1087 -> "11"
        1114 -> "13"
        1117 -> "13"
        1135 -> "82"
        1147 -> "82"
        1150 -> "10"
        1153 -> "10"
        1168 -> "10"
        1171 -> "10"
        1180 -> "10"
        1183 -> "10"
        1186 -> "10"
        1189 -> "10"
        1192 -> "10"
        1195 -> "10"
        1198 -> "81"
        1201 -> "81"
        1204 -> "81"
        1207 -> "81"
        1210 -> "13"
        1213 -> "13"
        1216 -> "13"
        1219 -> "13"
        1222 -> "13"
        1225 -> "13"
        1237 -> "13"
        1240 -> "10"
        1243 -> "10"
        1246 -> "10"
        1249 -> "81"
        1252 -> "81"
        1255 -> "13"
        1258 -> "13"
        1261 -> "13"
        1264 -> "13"
        1273 -> "11"
        1276 -> "09"
        1279 -> "13"
        1282 -> "13"
        else -> ""
    } + if (isDaytime) "d" else "n"

    fun getYRIcon(iconCode: String): String = when (iconCode.substringBefore('_')) {
        "clearsky" -> "01"
        "cloudy" -> "04"
        "fair" -> "02"
        "fog" -> "82"
        "heavyrain" -> "10"
        "heavyrainandthunder" -> "09"
        "heavyrainshowers" -> "10"
        "heavyrainshowersandthunder" -> "09"
        "heavysleet" -> "81"
        "heavysleetandthunder" -> "81"
        "heavysleetshowers" -> "81"
        "heavysleetshowersandthunder" -> "81"
        "heavysnow" -> "13"
        "heavysnowandthunder" -> "13"
        "heavysnowshowers" -> "13"
        "heavysnowshowersandthunder" -> "13"
        "lightrain" -> "10"
        "lightrainandthunder" -> "11"
        "lightrainshowers" -> "10"
        "lightrainshowersandthunder" -> "11"
        "lightsleet" -> "81"
        "lightsleetandthunder" -> "81"
        "lightsleetshowers" -> "81"
        "lightsnow" -> "13"
        "lightsnowandthunder" -> "13"
        "lightsnowshowers" -> "13"
        "lightssleetshowersandthunder" -> "81"
        "lightssnowshowersandthunder" -> "81"
        "partlycloudy" -> "03"
        "rain" -> "10"
        "rainandthunder" -> "11"
        "rainshowers" -> "10"
        "rainshowersandthunder" -> "11"
        "sleet" -> "81"
        "sleetandthunder" -> "81"
        "sleetshowers" -> "81"
        "sleetshowersandthunder" -> "81"
        "snow" -> "13"
        "snowandthunder" -> "13"
        "snowshowers" -> "13"
        "snowshowersandthunder" -> "13"
        else -> ""
    } + if (iconCode.substringAfter('_', "day") == "day") "d" else "n"

}
