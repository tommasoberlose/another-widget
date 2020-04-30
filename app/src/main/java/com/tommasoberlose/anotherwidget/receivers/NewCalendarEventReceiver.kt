package com.tommasoberlose.anotherwidget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.utils.CalendarUtil

class NewCalendarEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when {
            intent.action.equals(Intent.ACTION_PROVIDER_CHANGED) -> {
                CalendarUtil.updateEventList(context)
            }
            intent.action == Actions.ACTION_GO_TO_NEXT_EVENT -> {
                CalendarUtil.goToNextEvent(context)
            }
            intent.action == Actions.ACTION_GO_TO_PREVIOUS_EVENT -> {
                CalendarUtil.goToPreviousEvent(context)
            }
        }
    }
}
