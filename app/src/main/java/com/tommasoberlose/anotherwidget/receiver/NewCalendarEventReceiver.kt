package com.tommasoberlose.anotherwidget.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tommasoberlose.anotherwidget.util.Util

class NewCalendarEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Util.updateWidget(context)
    }
}
