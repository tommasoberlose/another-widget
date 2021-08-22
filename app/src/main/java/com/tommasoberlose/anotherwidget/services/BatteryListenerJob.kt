package com.tommasoberlose.anotherwidget.services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget

class BatteryListenerJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        MainWidget.updateWidget(this)
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
        private const val chargingJobId = 1006
        private const val notChargingJobId = 1007
        fun schedule(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                remove(context)
                val componentName = ComponentName(
                    context,
                    EventListenerJob::class.java
                )
                with(context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler) {
                    schedule(
                        JobInfo.Builder(chargingJobId, componentName)
                            .setRequiresCharging(true)
                            .setPersisted(true)
                            .build()
                    )
                    schedule(
                        JobInfo.Builder(notChargingJobId, componentName)
                            .setRequiresCharging(false)
                            .setPersisted(true)
                            .build()
                    )
                }
            }
        }

        private fun remove(context: Context) {
            val js = context.getSystemService(JobScheduler::class.java)
            js?.cancel(chargingJobId)
            js?.cancel(notChargingJobId)
        }
    }
}