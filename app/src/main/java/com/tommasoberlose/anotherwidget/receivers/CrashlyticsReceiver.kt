package com.tommasoberlose.anotherwidget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tommasoberlose.anotherwidget.global.Actions
import java.lang.Exception

class CrashlyticsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Actions.ACTION_REPORT_CRASH) {
            val exception: Exception = intent.getSerializableExtra(EXCEPTION) as Exception
            FirebaseCrashlytics.getInstance().recordException(exception)
            FirebaseCrashlytics.getInstance().sendUnsentReports()
        }
    }


    companion object {
        private const val EXCEPTION = "EXCEPTION"

        fun sendCrash(context: Context, exception: Exception) {
            context.sendBroadcast(Intent(context, CrashlyticsReceiver::class.java).apply {
                action = Actions.ACTION_REPORT_CRASH
                putExtra(EXCEPTION, exception)
            })
        }
    }

}