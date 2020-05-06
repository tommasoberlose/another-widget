package com.tommasoberlose.anotherwidget.services

import android.app.AlarmManager
import android.content.Context
import android.util.Log
import androidx.work.*
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.models.Event
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import org.joda.time.Period
import java.util.*
import java.util.concurrent.TimeUnit

class UpdatesWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        Log.d("ciao1", "update ok: $inputData")
        val repo = EventRepository(applicationContext)
        val event = repo.getEventByEventId(inputData.getLong(EVENT_ID, -1))
        event?.let {
            scheduleEventUpdate(applicationContext, it)
        }
        MainWidget.updateWidget(applicationContext)
        return Result.success()
    }

    companion object {
        private const val JOB_TAG = "UPDATES_WORKER"
        private const val EVENT_ID = "event_id"

        fun setUpdates(context: Context) {
            removeUpdates(context)

            EventRepository(context).getEvents().forEach { event ->
                scheduleEventUpdate(context, event)
            }
        }

        private fun scheduleEventUpdate(context: Context, event: Event) {
            val workManager = WorkManager.getInstance(context)
            val now = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (event.startDate > now.timeInMillis) {
                val diff = Period(now.timeInMillis, event.startDate)
                workManager.enqueueUniqueWork("UPDATES_JOB_ONE_TIME_${event.eventID}", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<WeatherWorker>()
                    .setInputData(Data.Builder().putLong(EVENT_ID, event.eventID).build())
                    .setInitialDelay(if (diff.minutes > 0) diff.minutes.toLong() else 60L, TimeUnit.MINUTES)
                    .addTag(JOB_TAG)
                    .build()
                )
                Log.d("ciao1", "next update ${Date(now.timeInMillis + (if (diff.minutes > 0) diff.minutes.toLong() else 60L) * 60 * 1000)}")
            } else if (event.endDate > now.timeInMillis) {
                // Update the widget one second after the event is finished
                workManager.enqueueUniqueWork("UPDATES_JOB_ONE_TIME_END_${event.eventID}", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<WeatherWorker>()
                    .setInitialDelay(event.endDate - now.timeInMillis, TimeUnit.MILLISECONDS)
                    .addTag(JOB_TAG)
                    .build()
                )
            }
        }

        fun removeUpdates(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(JOB_TAG)
        }
    }
}