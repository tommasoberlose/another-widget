package com.tommasoberlose.anotherwidget.helpers

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.FenceReceiver
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission


object DailyStepsHelper {
    fun registerFence(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || context.checkGrantedPermission(Manifest.permission.ACTIVITY_RECOGNITION)) {
            val transitions = mutableListOf<ActivityTransition>()

            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()

            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()

            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()

            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()

            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()

            val request = ActivityTransitionRequest(transitions)

            // myPendingIntent is the instance of PendingIntent where the app receives callbacks.
            val task = ActivityRecognition.getClient(context)
                .requestActivityTransitionUpdates(
                    request,
                    PendingIntent.getBroadcast(
                        context,
                        2,
                        Intent(context, FenceReceiver::class.java),
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
                    Intent(context, FenceReceiver::class.java),
                    0
                )
            )

        task.addOnCompleteListener {
            if (it.isSuccessful) {
                PendingIntent.getBroadcast(
                    context,
                    2,
                    Intent(context, FenceReceiver::class.java),
                    0
                ).cancel()

            }
        }
    }
}