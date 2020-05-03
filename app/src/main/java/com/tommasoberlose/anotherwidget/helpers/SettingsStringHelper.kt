package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import android.text.format.DateUtils
import com.tommasoberlose.anotherwidget.R
import org.joda.time.DateTime
import java.util.concurrent.TimeUnit

object SettingsStringHelper {


    fun getRefreshPeriodString(period: Int): Int {
        return when (period) {
            0 -> R.string.settings_weather_refresh_period_subtitle_0
            1 -> R.string.settings_weather_refresh_period_subtitle_1
            2 -> R.string.settings_weather_refresh_period_subtitle_2
            3 -> R.string.settings_weather_refresh_period_subtitle_3
            4 -> R.string.settings_weather_refresh_period_subtitle_4
            5 -> R.string.settings_weather_refresh_period_subtitle_5
            else -> R.string.settings_weather_refresh_period_subtitle_0
        }
    }

    fun getShowUntilString(period: Int): Int {
        return when (period) {
            0 -> R.string.settings_show_until_subtitle_0
            1 -> R.string.settings_show_until_subtitle_1
            2 -> R.string.settings_show_until_subtitle_2
            3 -> R.string.settings_show_until_subtitle_3
            4 -> R.string.settings_show_until_subtitle_4
            5 -> R.string.settings_show_until_subtitle_5
            6 -> R.string.settings_show_until_subtitle_6
            7 -> R.string.settings_show_until_subtitle_7
            else -> R.string.settings_show_until_subtitle_1
        }
    }

    fun getSecondRowInfoString(info: Int): Int {
        return when (info) {
            0 -> R.string.settings_second_row_info_subtitle_0
            1 -> R.string.settings_second_row_info_subtitle_1
            2 -> R.string.settings_second_row_info_subtitle_2
            else -> R.string.settings_second_row_info_subtitle_0
        }
    }

    fun getTextShadowString(shadow: Int): Int {
        return when (shadow) {
            0 -> R.string.settings_text_shadow_subtitle_none
            1 -> R.string.settings_text_shadow_subtitle_low
            2 -> R.string.settings_text_shadow_subtitle_high
            else -> R.string.settings_text_shadow_subtitle_low
        }
    }

    fun getCustomFontLabel(shadow: Int): Int {
        return when (shadow) {
            0 -> R.string.custom_font_subtitle_0
            1 -> R.string.custom_font_subtitle_1
            else -> R.string.custom_font_subtitle_1
        }
    }

    fun getDifferenceText(context: Context, now: Long, start: Long): String {
        val nowDate = DateTime(now)
        val eventDate = DateTime(start)

        var difference = start - now
        difference += 60 * 1000 - (difference % (60 * 1000))

        when {
            difference <= 0 || TimeUnit.MILLISECONDS.toHours(difference) < 1 -> {
                return ""
            }
            TimeUnit.MILLISECONDS.toHours(difference) < 12 -> {
                return DateUtils.getRelativeTimeSpanString(start, now, DateUtils.HOUR_IN_MILLIS).toString()
            }
            eventDate.dayOfYear == nowDate.plusDays(1).dayOfYear -> {
                return String.format("%s", context.getString(R.string.tomorrow))
            }
            eventDate.dayOfYear == nowDate.dayOfYear -> {
                return String.format("%s", context.getString(R.string.today))
            }
            else -> {
                return DateUtils.getRelativeTimeSpanString(start, now, DateUtils.DAY_IN_MILLIS).toString()
            }
        }

    }

    fun getAllDayEventDifferenceText(context: Context, now: Long, start: Long): String {
        val nowDate = DateTime(now)
        val eventDate = DateTime(start)

        var difference = start - now
        difference += 60 * 1000 - (difference % (60 * 1000))

        return when (eventDate.dayOfYear) {
            nowDate.dayOfYear -> {
                ""
            }
            nowDate.plusDays(1).dayOfYear -> {
                String.format("%s", context.getString(R.string.tomorrow))
            }
            else -> {
                DateUtils.getRelativeTimeSpanString(start, now, DateUtils.DAY_IN_MILLIS).toString()
            }
        }

    }

    fun getEmojiByUnicode(unicode: Int): String {
        return String(Character.toChars(unicode))
    }
}