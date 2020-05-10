package com.tommasoberlose.anotherwidget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field.FIELD_STEPS
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import java.util.*
import java.util.concurrent.TimeUnit


class FenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)!!
            val lastEvent = result.transitionEvents.last()

            if (lastEvent.activityType == DetectedActivity.WALKING || lastEvent.activityType == DetectedActivity.RUNNING) {
                requestDailySteps(context)
            } else {
                resetDailySteps()
            }
        }
    }

    private fun requestDailySteps(context: Context) {
        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .build()

        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

        Log.d("ciao", "hasPermission: ${GoogleSignIn.hasPermissions(account, fitnessOptions)}")

        if (GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            val cal: Calendar = Calendar.getInstance()
            cal.time = Date()
            val endTime: Long = cal.timeInMillis
            cal.add(Calendar.YEAR, -1)
            val startTime: Long = cal.timeInMillis
            val readRequest = DataReadRequest.Builder()
                .aggregate(
                    DataType.TYPE_STEP_COUNT_DELTA,
                    DataType.AGGREGATE_STEP_COUNT_DELTA
                )
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .bucketByTime(1, TimeUnit.DAYS)
                .build()

            if (account != null) {
                Fitness.getHistoryClient(context, account)
                    .readData(readRequest)
                    .addOnSuccessListener { response ->
                        Preferences.googleFitSteps =
                            response.dataSets[0].dataPoints[0].getValue(FIELD_STEPS).asFloat()
                                .toLong()
                        Log.d("ciao",
                            "response: ${response.dataSets[0].dataPoints[0].getValue(FIELD_STEPS)
                                .asFloat().toLong()}"
                        )
                        MainWidget.updateWidget(context)
                    }
            }
        }
    }

    private fun resetDailySteps() {
        Preferences.googleFitSteps = -1
    }
}