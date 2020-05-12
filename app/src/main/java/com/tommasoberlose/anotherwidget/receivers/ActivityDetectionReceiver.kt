package com.tommasoberlose.anotherwidget.receivers

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field.FIELD_STEPS
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.location.*
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import java.util.*
import java.util.concurrent.TimeUnit


class ActivityDetectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)!!
            val lastEvent = result.transitionEvents.last()

            Log.d("ciao", "activity detected: $lastEvent")
            if (lastEvent.activityType == DetectedActivity.WALKING || lastEvent.activityType == DetectedActivity.RUNNING && lastEvent.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                requestDailySteps(context)
            }
        } else {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED && Preferences.showDailySteps && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || context.checkGrantedPermission(Manifest.permission.ACTIVITY_RECOGNITION)) {
                resetDailySteps()
                registerFence(context)
            } else {
                resetDailySteps()
            }
        }
    }

    private fun resetDailySteps() {
        Preferences.googleFitSteps = -1
    }

    companion object {
        val FITNESS_OPTIONS: FitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .build()

        fun registerFence(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || context.checkGrantedPermission(
                    Manifest.permission.ACTIVITY_RECOGNITION)) {
                val transitions = mutableListOf<ActivityTransition>()

                transitions +=
                    ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()

                transitions +=
                    ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.RUNNING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()

                val request = ActivityTransitionRequest(transitions)

                // myPendingIntent is the instance of PendingIntent where the app receives callbacks.
                val task = ActivityRecognition.getClient(context)
                    .requestActivityTransitionUpdates(
                        request,
                        PendingIntent.getBroadcast(
                            context,
                            2,
                            Intent(context, ActivityDetectionReceiver::class.java),
                            0
                        )
                    )

                task.addOnFailureListener { e: Exception ->
                    e.printStackTrace()
                    Preferences.showDailySteps = false
                }

            }
        }

        fun unregisterFence(context: Context) {
            val task = ActivityRecognition.getClient(context)
                .removeActivityTransitionUpdates(
                    PendingIntent.getBroadcast(
                        context,
                        2,
                        Intent(context, ActivityDetectionReceiver::class.java),
                        0
                    )
                )

            task.addOnCompleteListener {
                if (it.isSuccessful) {
                    PendingIntent.getBroadcast(
                        context,
                        2,
                        Intent(context, ActivityDetectionReceiver::class.java),
                        0
                    ).cancel()
                }
            }
        }

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
                            try {
                                it.getDataSet(DataType.AGGREGATE_STEP_COUNT_DELTA)?.dataPoints?.get(
                                    0
                                )?.getValue(FIELD_STEPS)?.asInt() ?: 0
                            } catch (ex: Exception) {
                                0
                            }
                        }.toLong()
                        MainWidget.updateWidget(context)
                        setTimeout(context)
                    }
            }
        }

        private fun setTimeout(context: Context) {
            with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                cancel(PendingIntent.getBroadcast(context, 5, Intent(context, ActivityDetectionReceiver::class.java), 0))
                setExactAndAllowWhileIdle(
                    AlarmManager.RTC,
                    Calendar.getInstance().timeInMillis + 5 * 60 * 1000,
                    PendingIntent.getBroadcast(
                        context,
                        5,
                        Intent(context, ActivityDetectionReceiver::class.java),
                        0
                    )
                )
            }
        }
    }
}