package com.tommasoberlose.anotherwidget.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NewCalendarEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Util.updateWidget(context)
    }
}
