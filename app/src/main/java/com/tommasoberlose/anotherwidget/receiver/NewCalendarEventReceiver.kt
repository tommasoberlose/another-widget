package com.tommasoberlose.anotherwidget.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tommasoberlose.anotherwidget.util.CalendarUtil
import com.tommasoberlose.anotherwidget.util.Util

class NewCalendarEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_PROVIDER_CHANGED)) {
            CalendarUtil.updateEventList(context)
        }
    }
}
