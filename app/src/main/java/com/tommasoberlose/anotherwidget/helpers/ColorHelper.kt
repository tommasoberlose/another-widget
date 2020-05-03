package com.tommasoberlose.anotherwidget.helpers

import android.graphics.Color
import com.tommasoberlose.anotherwidget.global.Preferences

object ColorHelper {
    fun getFontColor(): Int {
        return try {
            Color.parseColor(Preferences.textGlobalColor)
        } catch (e: Exception) {
            Color.parseColor("#FFFFFF")
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
}