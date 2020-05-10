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
import com.google.android.gms.fitness.data.Field
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

    private fun resetDailySteps() {
        Preferences.googleFitSteps = -1
    }

    companion object {
        val FITNESS_OPTIONS = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .build()

        fun requestDailySteps(context: Context) {

            val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && GoogleSignIn.hasPermissions(account, FITNESS_OPTIONS)) {

                val cal: Calendar = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startTime: Long = cal.timeInMillis

                cal.add(Calendar.DAY_OF_YEAR, 1)
                val endTime: Long = cal.timeInMillis
                
                val readRequest = DataReadRequest.Builder()
                    .aggregate(
                        DataType.TYPE_STEP_COUNT_DELTA,
                        DataType.AGGREGATE_STEP_COUNT_DELTA
                    )
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .build()

                Fitness.getHistoryClient(context, account)
                    .readData(readRequest)
                    .addOnSuccessListener { response ->
                        Preferences.googleFitSteps = response.buckets.sumBy {
                            it.getDataSet(DataType.AGGREGATE_STEP_COUNT_DELTA)?.dataPoints?.get(0)?.getValue(FIELD_STEPS)?.asInt() ?: 0
                        }.toLong()
                        MainWidget.updateWidget(context)
                    }
            }
        }
    }
}