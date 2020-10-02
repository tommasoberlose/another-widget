package com.tommasoberlose.anotherwidget.helpers

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import com.tommasoberlose.anotherwidget.global.Preferences
import kotlin.math.roundToInt

object ColorHelper {
    fun getFontColor(isDark: Boolean): Int {
        return try {
            Color.parseColor("#%s%s".format(if (!isDark) Preferences.textGlobalAlpha else Preferences.textGlobalAlphaDark, (if (!isDark) Preferences.textGlobalColor else Preferences.textGlobalColorDark).replace("#", "")))
        } catch (e: Exception) {
            Color.parseColor("#FFFFFFFF")
        }
    }

    fun getFontColorAlpha(isDark: Boolean): Int {
        return try {
            (if (!isDark) Preferences.textGlobalAlpha else Preferences.textGlobalAlphaDark).toIntValue().toDouble() * 255 / 100
        } catch (e: Exception) {
            "FF".toIntValue().toDouble() * 255 / 100
        }.roundToInt()
    }

    fun getFontColorRgb(isDark: Boolean): Int {
        return try {
            Color.parseColor((if (!isDark) Preferences.textGlobalColor else Preferences.textGlobalColorDark))
        } catch (e: Exception) {
            Color.parseColor("#000000")
        }
    }

    fun getSecondaryFontColor(isDark: Boolean): Int {
        return try {
            Color.parseColor("#%s%s".format((if (!isDark) Preferences.textSecondaryAlpha else Preferences.textSecondaryAlphaDark), (if (!isDark) Preferences.textSecondaryColor else Preferences.textSecondaryColorDark).replace("#", "")))
        } catch (e: Exception) {
            Color.parseColor("#FFFFFFFF")
        }
    }

    fun getSecondaryFontColorAlpha(isDark: Boolean): Int {
        return try {
            (if (!isDark) Preferences.textSecondaryAlpha else Preferences.textSecondaryAlphaDark).toIntValue().toDouble() * 255 / 100
        } catch (e: Exception) {
            "FF".toIntValue().toDouble() * 255 / 100
        }.roundToInt()
    }

    fun getSecondaryFontColorRgb(isDark: Boolean): Int {
        return try {
            Color.parseColor((if (!isDark) Preferences.textSecondaryColor else Preferences.textSecondaryColorDark))
        } catch (e: Exception) {
            Color.parseColor("#000000")
        }
    }

    fun getClockFontColor(isDark: Boolean): Int {
        return try {
            Color.parseColor("#%s%s".format((if (!isDark) Preferences.clockTextAlpha else Preferences.clockTextAlphaDark), (if (!isDark) Preferences.clockTextColor else Preferences.clockTextColorDark).replace("#", "")))
        } catch (e: Exception) {
            Color.parseColor("#FFFFFFFF")
        }
    }

    fun getClockFontColorAlpha(isDark: Boolean): Int {
        return try {
            (if (!isDark) Preferences.clockTextAlpha else Preferences.clockTextAlphaDark).toIntValue().toDouble() * 255 / 100
        } catch (e: Exception) {
            "FF".toIntValue().toDouble() * 255 / 100
        }.roundToInt()
    }

    fun getClockFontColorRgb(isDark: Boolean): Int {
        return try {
            Color.parseColor((if (!isDark) Preferences.clockTextColor else Preferences.clockTextColorDark))
        } catch (e: Exception) {
            Color.parseColor("#000000")
        }
    }

    fun getBackgroundColor(isDark: Boolean): Int {
        return try {
            Color.parseColor("#%s%s".format((if (!isDark) Preferences.backgroundCardAlpha else Preferences.backgroundCardAlphaDark), (if (!isDark) Preferences.backgroundCardColor else Preferences.backgroundCardColorDark).replace("#", "")))
        } catch (e: Exception) {
            Color.parseColor("#00000000")
        }
    }

    fun getBackgroundAlpha(isDark: Boolean): Int {
        return try {
            (if (!isDark) Preferences.backgroundCardAlpha else Preferences.backgroundCardAlphaDark).toIntValue().toDouble() * 255 / 100
        } catch (e: Exception) {
            "00".toIntValue().toDouble() * 255 / 100
        }.roundToInt()
    }

    fun getBackgroundColorRgb(isDark: Boolean): Int {
        return try {
            Color.parseColor((if (!isDark) Preferences.backgroundCardColor else Preferences.backgroundCardColorDark))
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