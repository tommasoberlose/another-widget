package com.tommasoberlose.anotherwidget.helpers

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.graphics.Color
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.utils.toast
import kotlin.math.roundToInt


object ColorHelper {
    fun getFontColor(isDark: Boolean): Int {
        return try {
            Color.parseColor("#%s%s".format(if (!isDark) Preferences.textGlobalAlpha else Preferences.textGlobalAlphaDark,
                (if (!isDark) Preferences.textGlobalColor else Preferences.textGlobalColorDark).replace(
                    "#",
                    "")))
        } catch (e: Exception) {
            Color.parseColor("#FFFFFFFF")
        }
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
            Color.parseColor("#%s%s".format((if (!isDark) Preferences.textSecondaryAlpha else Preferences.textSecondaryAlphaDark),
                (if (!isDark) Preferences.textSecondaryColor else Preferences.textSecondaryColorDark).replace(
                    "#",
                    "")))
        } catch (e: Exception) {
            Color.parseColor("#FFFFFFFF")
        }
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
            Color.parseColor("#%s%s".format((if (!isDark) Preferences.clockTextAlpha else Preferences.clockTextAlphaDark),
                (if (!isDark) Preferences.clockTextColor else Preferences.clockTextColorDark).replace(
                    "#",
                    "")))
        } catch (e: Exception) {
            Color.parseColor("#FFFFFFFF")
        }
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
            Color.parseColor("#%s%s".format((if (!isDark) Preferences.backgroundCardAlpha else Preferences.backgroundCardAlphaDark),
                (if (!isDark) Preferences.backgroundCardColor else Preferences.backgroundCardColorDark).replace(
                    "#",
                    "")))
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

    fun String.isColor(): Boolean {
        return try {
            Color.parseColor(this)
            true
        } catch (iae: IllegalArgumentException) {
            iae.printStackTrace()
            false
        }
    }

    @SuppressLint("DefaultLocale")
    fun Context.copyToClipboard(color: Int?, alpha: Int) {
        if (color == null) return toast(getString(R.string.error_copy_color))
        with(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager) {
            try {
                val colorString = Integer.toHexString(color)
                val clip = "#%s%s".format(
                    alpha.toHexValue(),
                    if (colorString.length > 6) colorString.substring(2) else colorString
                ).toUpperCase()
                setPrimaryClip(ClipData.newPlainText(clip, clip))
                toast(getString(R.string.color_copied))
            } catch (ex: Exception) {
                ex.printStackTrace()
                toast(getString(R.string.error_copy_color))
            }
        }
    }

    fun Context.isClipboardColor(): Boolean {
        with(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager) {
            return try { primaryClip?.getItemAt(0)?.text?.toString()?.isColor() ?: false } catch (ex: Exception) { false }
        }
    }

    fun Context.pasteFromClipboard(pasteColor: (color: String, alpha: String) -> Unit) {
        with(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager) {
            primaryClip?.let {
                val item = it.getItemAt(0).text.toString().replace("#", "")
                val color = if (item.length > 6) item.substring(2) else item
                val alpha = if (item.length > 6) item.substring(0, 2) else "00"
                pasteColor("#$color", alpha)
            }
        }
    }
}