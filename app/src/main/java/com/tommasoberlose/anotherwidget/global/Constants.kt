package com.tommasoberlose.anotherwidget.global

import java.text.SimpleDateFormat

object Constants {
    const val RESULT_CODE_CUSTOM_LOCATION = 45
    const val RESULT_APP_NAME = "RESULT_APP_NAME"
    const val RESULT_APP_PACKAGE = "RESULT_APP_PACKAGE"

    const val CUSTOM_FONT_GOOGLE_SANS = 1
    const val CUSTOM_FONT_DOWNLOADED = 2
    const val CUSTOM_FONT_DOWNLOAD_NEW = 3

    enum class ClockBottomMargin(val value: Int) {
        NONE(0),
        SMALL(1),
        MEDIUM(2),
        LARGE(3)
    }

    enum class SecondRowTopMargin(val value: Int) {
        NONE(0),
        SMALL(1),
        MEDIUM(2),
        LARGE(3)
    }

    enum class GlanceProviderId(val id: String) {
        PLAYING_SONG("PLAYING_SONG"),
        NEXT_CLOCK_ALARM("NEXT_CLOCK_ALARM"),
        BATTERY_LEVEL_LOW("BATTERY_LEVEL_LOW"),
        CUSTOM_INFO("CUSTOM_INFO"),
        GOOGLE_FIT_STEPS("GOOGLE_FIT_STEPS"),
        NOTIFICATIONS("NOTIFICATIONS"),
        GREETINGS("GREETINGS");

        companion object {
            private val map = GlanceProviderId.values().associateBy(GlanceProviderId::id)
            fun from(type: String) = map[type]
        }
    }

    enum class WidgetUpdateFrequency(val value: Int) {
        LOW(0),
        DEFAULT(1),
        HIGH(2)
    }

    enum class WeatherProvider(val value: Int) {
        OPEN_WEATHER(0),
        WEATHER_BIT(1),
        WEATHER_API(2),
        HERE(3),
        ACCUWEATHER(4),
        WEATHER_GOV(5),
        YR(6);

        companion object {
            private val map = WeatherProvider.values().associateBy(WeatherProvider::value)
            fun fromInt(type: Int) = map[type]
        }
    }

    enum class WeatherIconPack(val value: Int) {
        DEFAULT(0),
        MINIMAL(1),
        COOL(2),
        GOOGLE_NEWS(3)
    }
}