package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.os.BatteryManager
import com.tommasoberlose.anotherwidget.global.Preferences


object BatteryHelper {
    fun updateBatteryInfo(context: Context) {
        with(context.getSystemService(BATTERY_SERVICE) as BatteryManager) {
            Preferences.isBatteryLevelLow = getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) <= 15
            Preferences.isCharging = isCharging
        }
    }

    fun getBatteryLevel(context: Context): Int {
        with(context.getSystemService(BATTERY_SERVICE) as BatteryManager) {
            return getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }
    }
}