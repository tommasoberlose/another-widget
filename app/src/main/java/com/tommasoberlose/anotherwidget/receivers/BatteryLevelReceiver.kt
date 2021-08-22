package com.tommasoberlose.anotherwidget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget

class BatteryLevelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            Intent.ACTION_BATTERY_LOW -> Preferences.isBatteryLevelLow = true
            Intent.ACTION_BATTERY_OKAY -> Preferences.isBatteryLevelLow = false
            Intent.ACTION_POWER_CONNECTED -> Preferences.isCharging = true
            Intent.ACTION_POWER_DISCONNECTED -> Preferences.isCharging = false
        }
        MainWidget.updateWidget(context)
    }

}