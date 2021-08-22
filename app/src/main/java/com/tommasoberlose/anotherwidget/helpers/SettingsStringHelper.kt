package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import android.text.format.DateUtils
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
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

    fun getCustomFontLabel(context: Context, font: Int): String {
        return when (font) {
            Constants.CUSTOM_FONT_GOOGLE_SANS -> context.getString(R.string.custom_font_subtitle_1) + " - ${getVariantLabel(context, Preferences.customFontVariant)}"
            Constants.CUSTOM_FONT_DOWNLOADED -> Preferences.customFontName + " - ${getVariantLabel(context, Preferences.customFontVariant)}"
            else -> context.getString(R.string.custom_font_subtitle_0)
        }
    }

    fun getVariantLabel(context: Context, variant: String): String = when {
        variant == "italic" -> context.getString(R.string.font_italic)
        variant.contains("100") && variant.contains("italic") -> context.getString(R.string.font_100_italic)
        variant.contains("200") && variant.contains("italic") -> context.getString(R.string.font_200_italic)
        variant.contains("300") && variant.contains("italic") -> context.getString(R.string.font_300_italic)
        variant.contains("400") && variant.contains("italic") -> context.getString(R.string.font_400_italic)
        variant.contains("500") && variant.contains("italic") -> context.getString(R.string.font_500_italic)
        variant.contains("600") && variant.contains("italic") -> context.getString(R.string.font_600_italic)
        variant.contains("700") && variant.contains("italic") -> context.getString(R.string.font_700_italic)
        variant.contains("800") && variant.contains("italic") -> context.getString(R.string.font_800_italic)
        variant.contains("900") && variant.contains("italic") -> context.getString(R.string.font_900_italic)
        variant == "regular" || variant.contains("400") -> context.getString(R.string.font_400)
        variant.contains("100") -> context.getString(R.string.font_100)
        variant.contains("200") -> context.getString(R.string.font_200)
        variant.contains("300") -> context.getString(R.string.font_300)
        variant.contains("500") -> context.getString(R.string.font_500)
        variant.contains("600") -> context.getString(R.string.font_600)
        variant.contains("700") -> context.getString(R.string.font_700)
        variant.contains("800") -> context.getString(R.string.font_800)
        variant.contains("900") -> context.getString(R.string.font_900)
        else -> context.getString(R.string.font_400)
    }

    fun getDifferenceText(context: Context, now: Long, start: Long): String {
        val nowDate = DateTime(now)
        val eventDate = DateTime(start)

        var difference = start - now
        difference += 60 * 1000 - (difference % (60 * 1000))

        when {
            difference <= 0 -> {
                return ""
            }
            TimeUnit.MILLISECONDS.toHours(difference) < 1 && Preferences.widgetUpdateFrequency == Constants.WidgetUpdateFrequency.HIGH.rawValue && TimeUnit.MILLISECONDS.toMinutes(difference) > 5 -> {
                return DateUtils.getRelativeTimeSpanString(start, start - 1000 * 60 * (TimeUnit.MILLISECONDS.toMinutes(difference) - 1 - (TimeUnit.MILLISECONDS.toMinutes(difference) - 1) % 5), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
            }
            TimeUnit.MILLISECONDS.toHours(difference) < 1 && Preferences.widgetUpdateFrequency == Constants.WidgetUpdateFrequency.DEFAULT.rawValue && TimeUnit.MILLISECONDS.toMinutes(difference) > 5 -> {
                return DateUtils.getRelativeTimeSpanString(start, start - 1000 * 60 * (TimeUnit.MILLISECONDS.toMinutes(difference) - 1 - (TimeUnit.MILLISECONDS.toMinutes(difference) - 1) % 15), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
            }
            TimeUnit.MILLISECONDS.toHours(difference) < 1 && Preferences.widgetUpdateFrequency == Constants.WidgetUpdateFrequency.LOW.rawValue -> {
                return context.getString(R.string.soon)
            }
            TimeUnit.MILLISECONDS.toHours(difference) < 1 -> {
                return context.getString(R.string.now)
            }
            TimeUnit.MILLISECONDS.toHours(difference) < 12 -> {
                val minutes =  TimeUnit.MILLISECONDS.toMinutes(difference) - 60 * TimeUnit.MILLISECONDS.toHours(difference)
                return if (minutes < 1 || minutes > 30) {
                    DateUtils.getRelativeTimeSpanString(
                        start,
                        now - 1000 * 60 * 40,
                        DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                    ).toString()
                } else {
                    DateUtils.getRelativeTimeSpanString(
                        start,
                        now,
                        DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                    ).toString()
                }
            }
            eventDate.dayOfYear == nowDate.plusDays(1).dayOfYear -> {
                return String.format("%s", context.getString(R.string.tomorrow))
            }
            eventDate.dayOfYear == nowDate.dayOfYear -> {
                return String.format("%s", context.getString(R.string.today))
            }
            else -> {
                return DateUtils.getRelativeTimeSpanString(start, now, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
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
}