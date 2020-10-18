package com.tommasoberlose.anotherwidget.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import java.util.*


object GreetingsHelper {
    private const val MORNING_TIME = 36
    private const val MORNING_TIME_END = 37
    private const val EVENING_TIME = 38
    private const val NIGHT_TIME = 39

    fun toggleGreetings(context: Context) {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val now = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.HOUR_OF_DAY, 0)
            }
            if (Preferences.showGreetings) {
                setRepeating(
                    AlarmManager.RTC,
                    now.apply {
                        set(Calendar.HOUR_OF_DAY, 5)
                    }.timeInMillis,
                    1000 * 60 * 60 * 24,
                    PendingIntent.getBroadcast(context,
                        MORNING_TIME,
                        Intent(context, UpdatesReceiver::class.java).apply {
                            action = Actions.ACTION_UPDATE_GREETINGS
                        },
                        0)
                )

                setRepeating(
                    AlarmManager.RTC,
                    now.apply {
                        set(Calendar.HOUR_OF_DAY, 9)
                    }.timeInMillis,
                    1000 * 60 * 60 * 24,
                    PendingIntent.getBroadcast(context,
                        MORNING_TIME_END,
                        Intent(context, UpdatesReceiver::class.java).apply {
                            action = Actions.ACTION_UPDATE_GREETINGS
                        },
                        0)
                )

                setRepeating(
                    AlarmManager.RTC,
                    now.apply {
                        set(Calendar.HOUR_OF_DAY, 19)
                    }.timeInMillis,
                    1000 * 60 * 60 * 24,
                    PendingIntent.getBroadcast(context,
                        EVENING_TIME,
                        Intent(context, UpdatesReceiver::class.java).apply {
                            action = Actions.ACTION_UPDATE_GREETINGS
                        },
                        0)
                )

                setRepeating(
                    AlarmManager.RTC,
                    now.apply {
                        set(Calendar.HOUR_OF_DAY, 22)
                    }.timeInMillis,
                    1000 * 60 * 60 * 24,
                    PendingIntent.getBroadcast(context,
                        NIGHT_TIME,
                        Intent(context, UpdatesReceiver::class.java).apply {
                            action = Actions.ACTION_UPDATE_GREETINGS
                        },
                        0)
                )
            } else {
                listOf(MORNING_TIME, MORNING_TIME_END, EVENING_TIME, NIGHT_TIME).forEach {
                    cancel(PendingIntent.getBroadcast(context, it, Intent(context,
                        UpdatesReceiver::class.java).apply {
                        action = Actions.ACTION_UPDATE_GREETINGS
                    }, 0))
                }
            }
        }
    }

    fun showGreetings(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour < 9 || hour >= 19
    }

    fun getRandomString(context: Context): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val array = when {
            hour in 5..8 -> context.resources.getStringArray(R.array.morning_greetings)
            hour in 19..21 -> context.resources.getStringArray(R.array.evening_greetings)
            hour >= 22 && hour < 5 -> context.resources.getStringArray(R.array.night_greetings)
            else -> emptyArray()
        }
        return if (array.isNotEmpty()) array[Random().nextInt(array.size)] else ""
    }
}