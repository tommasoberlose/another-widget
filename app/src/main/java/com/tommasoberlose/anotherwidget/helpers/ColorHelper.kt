package com.tommasoberlose.anotherwidget.helpers

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import com.tommasoberlose.anotherwidget.global.Preferences
import kotlin.math.roundToInt

object ColorHelper {
    fun getFontColor(): Int {
        return try {
            Color.parseColor("#%s%s".format(Preferences.textGlobalAlpha, Preferences.textGlobalColor.replace("#", "")))
        } catch (e: Exception) {
            Color.parseColor("#FFFFFFFF")
        }
    }

    fun getFontColorAlpha(): Int {
        return try {
            Preferences.textGlobalAlpha.toIntValue().toDouble() * 255 / 100
        } catch (e: Exception) {
            "FF".toIntValue().toDouble() * 255 / 100
        }.roundToInt()
    }

    fun getFontColorRgb(): Int {
        return try {
            Color.parseColor(Preferences.textGlobalColor)
        } catch (e: Exception) {
            Color.parseColor("#000000")
        }
    }

    fun getSecondaryFontColor(): Int {
        return try {
            Color.parseColor("#%s%s".format(Preferences.textSecondaryAlpha, Preferences.textSecondaryColor.replace("#", "")))
        } catch (e: Exception) {
            Color.parseColor("#FFFFFFFF")
        }
    }

    fun getSecondaryFontColorAlpha(): Int {
        return try {
            Preferences.textSecondaryAlpha.toIntValue().toDouble() * 255 / 100
        } catch (e: Exception) {
            "FF".toIntValue().toDouble() * 255 / 100
        }.roundToInt()
    }

    fun getSecondaryFontColorRgb(): Int {
        return try {
            Color.parseColor(Preferences.textSecondaryColor)
        } catch (e: Exception) {
            Color.parseColor("#000000")
        }
    }

    fun getClockFontColor(): Int {
        return try {
            Color.parseColor("#%s%s".format(Preferences.clockTextAlpha, Preferences.clockTextColor.replace("#", "")))
        } catch (e: Exception) {
            Color.parseColor("#FFFFFFFF")
        }
    }

    fun getClockFontColorAlpha(): Int {
        return try {
            Preferences.clockTextAlpha.toIntValue().toDouble() * 255 / 100
        } catch (e: Exception) {
            "FF".toIntValue().toDouble() * 255 / 100
        }.roundToInt()
    }

    fun getClockFontColorRgb(): Int {
        return try {
            Color.parseColor(Preferences.clockTextColor)
        } catch (e: Exception) {
            Color.parseColor("#000000")
        }
    }

    fun getBackgroundColor(): Int {
        return try {
            Color.parseColor("#%s%s".format(Preferences.backgroundCardAlpha, Preferences.backgroundCardColor.replace("#", "")))
        } catch (e: Exception) {
            Color.parseColor("#00000000")
        }
    }

    fun getBackgroundAlpha(): Int {
        return try {
            Preferences.backgroundCardAlpha.toIntValue().toDouble() * 255 / 100
        } catch (e: Exception) {
            "00".toIntValue().toDouble() * 255 / 100
        }.roundToInt()
    }

    fun getBackgroundColorRgb(): Int {
        return try {
            Color.parseColor(Preferences.backgroundCardColor)
        } catch (e: Exception) {
            Color.parseColor("#000000")
        }
    }

    fun Int.isColorDark(threshold: Double = 0.5): Boolean {
        if (this == Color.TRANSPARENT) {
            return false
        }
        val darkness =
            1 - (0.299 * Color.red(this) + 0.587 * Color.green(this) + 0.114 * Color.blue(this)) / 255
        return darkness >= threshold
    }

    @SuppressLint("DefaultLocale")
    fun Int.toHexValue(): String {
        val intValue = (this * 255 / 100).toDouble().roundToInt()
        val hexValue = intValue.toString(16)
        return hexValue.padStart(2, '0').toUpperCase()
    }

    fun String.toIntValue(): Int {
        val hexValue = this.toInt(16).toDouble()
        return (hexValue * 100 / 255).roundToInt()
    }
}