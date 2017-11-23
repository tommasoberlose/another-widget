package com.tommasoberlose.anotherwidget.util

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.content.BroadcastReceiver
import android.content.Context
import com.tommasoberlose.anotherwidget.`object`.Constants
import android.content.IntentFilter
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.ui.activity.MainActivity


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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this, "Config")
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setPriority(Notification.PRIORITY_MIN)
                    .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    .setContentTitle(this.getString(R.string.background_service_title))
                    .setContentText(this.getString(R.string.background_service_subtitle))
                    .setAutoCancel(true);
            startForeground(5, mBuilder.build())
        }

        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
