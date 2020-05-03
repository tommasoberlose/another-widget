package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.utils.getCapWordString
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

object DateHelper {
    fun getDateText(context: Context, date: Calendar): String {
        return if (Preferences.dateFormat != "") {
            try {
                SimpleDateFormat(Preferences.dateFormat, Locale.getDefault()).format(date.time)
            } catch (e: Exception) {
                getDefaultDateText(context, date)
            }
        } else {
            val flags: Int =
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_MONTH
            "%s, %s".format(
                SimpleDateFormat("EEEE", Locale.getDefault()).format(date.time),
                DateUtils.formatDateTime(context, date.timeInMillis, flags)
            ).getCapWordString()
        }
    }

    fun getDefaultDateText(context: Context, date: Calendar): String {
        val flags: Int =
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_MONTH
        return "%s, %s".format(
            SimpleDateFormat("EEEE", Locale.getDefault()).format(date.time),
            DateUtils.formatDateTime(context, date.timeInMillis, flags)
        ).getCapWordString()
    }
}