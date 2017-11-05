package com.tommasoberlose.anotherwidget.util

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.content.BroadcastReceiver
import android.content.Context
import com.tommasoberlose.anotherwidget.`object`.Constants
import android.content.IntentFilter
import android.util.Log


class CrocodileService : Service() {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_TIME_TICK && isScreenOn) {
                sendBroadcast(Intent(Constants.ACTION_TIME_UPDATE))
            } else if (intent.action == Intent.ACTION_SCREEN_OFF) {
                isScreenOn = false
            } else if (intent.action == Intent.ACTION_SCREEN_ON) {
                isScreenOn = true
                sendBroadcast(Intent(Constants.ACTION_TIME_UPDATE))
            }
        }
    }

    private var isScreenOn = true

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
