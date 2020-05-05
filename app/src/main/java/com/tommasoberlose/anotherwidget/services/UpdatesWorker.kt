package com.tommasoberlose.anotherwidget.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import org.joda.time.Period
import java.util.*
import java.util.concurrent.TimeUnit


class UpdatesWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        CalendarHelper.updateEventList(applicationContext)
        return Result.success()
    }

    companion object {
        private const val JOB_TAG = "UPDATES_WORKER"

        fun setUpdates(context: Context) {
            removeUpdates(context)
            val now = Calendar.getInstance().timeInMillis
            val workManager = WorkManager.getInstance(context)
            val eventRepository = EventRepository(context)
                eventRepository.getEvents().forEach { event ->
                val hoursDiff = Period(Calendar.getInstance().timeInMillis, event.startDate).hours

                // Update the widget every hour till the event
                (0 .. hoursDiff).forEach {
                    workManager.enqueue(OneTimeWorkRequestBuilder<UpdatesWorker>().setInitialDelay((event.startDate + 1000) - now - it * 1000 * 60* 60, TimeUnit.MILLISECONDS).build())
                }

                // Update the widget one second after the event is finished
                workManager.enqueue(OneTimeWorkRequestBuilder<UpdatesWorker>().setInitialDelay(event.endDate + 1000 - now, TimeUnit.MILLISECONDS).build())
            }
        }

        fun removeUpdates(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(JOB_TAG)
        }
    }
}