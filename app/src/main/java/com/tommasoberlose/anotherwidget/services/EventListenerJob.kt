package com.tommasoberlose.anotherwidget.services

import android.app.job.JobInfo
import android.app.job.JobInfo.TriggerContentUri
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.CalendarContract
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper


class EventListenerJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        CalendarHelper.updateEventList(this)
        schedule(
            this
        )
        return false
    }

    @Synchronized
    override fun onStopJob(params: JobParameters): Boolean {
        return false
    }

    companion object {
        private const val jobId = 1005
        fun schedule(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val componentName = ComponentName(
                    context,
                    EventListenerJob::class.java
                )
                with(context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler) {
                    schedule(
                        JobInfo.Builder(jobId, componentName)
                            .addTriggerContentUri(TriggerContentUri(
                                CalendarContract.CONTENT_URI,
                                TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS
                            ))
                            .build()
                    )
                }
            }
        }

        fun remove(context: Context) {
            val js = context.getSystemService(JobScheduler::class.java)
            js?.cancel(jobId)
        }
    }
}