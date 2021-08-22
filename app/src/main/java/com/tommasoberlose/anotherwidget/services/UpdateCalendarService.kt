package com.tommasoberlose.anotherwidget.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper.applyFilters
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper.sortEvents
import com.tommasoberlose.anotherwidget.models.Event
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.fragments.MainFragment
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.everything.providers.android.calendar.CalendarProvider
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList

class UpdateCalendarService : Service() {

    companion object {
        const val CALENDAR_SYNC_NOTIFICATION_ID = 28468
        fun enqueueWork(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, UpdateCalendarService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(CALENDAR_SYNC_NOTIFICATION_ID, getCalendarSyncNotification())
    }

    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(CALENDAR_SYNC_NOTIFICATION_ID, getCalendarSyncNotification())
        job?.cancel()
        job = GlobalScope.launch(Dispatchers.IO) {

            val eventRepository = EventRepository(this@UpdateCalendarService)
            if (Preferences.showEvents) {
                val eventList = ArrayList<Event>()

                val now = Calendar.getInstance()
                val begin = Calendar.getInstance().apply {
                    set(Calendar.MILLISECOND, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.HOUR_OF_DAY, 0)
                }
                val limit = Calendar.getInstance().apply {
                    when (Preferences.showUntil) {
                        0 -> add(Calendar.HOUR, 3)
                        1 -> add(Calendar.HOUR, 6)
                        2 -> add(Calendar.HOUR, 12)
                        3 -> add(Calendar.DAY_OF_MONTH, 1)
                        4 -> add(Calendar.DAY_OF_MONTH, 3)
                        5 -> add(Calendar.DAY_OF_MONTH, 7)
                        6 -> add(Calendar.MINUTE, 30)
                        7 -> add(Calendar.HOUR, 1)
                        else -> add(Calendar.HOUR, 6)
                    }
                }

                if (!checkGrantedPermission(
                        Manifest.permission.READ_CALENDAR
                    )
                ) {
                    eventRepository.resetNextEventData()
                } else {
                    try {
                        val provider = CalendarProvider(this@UpdateCalendarService)
                        val data = provider.getInstances(begin.timeInMillis, limit.timeInMillis)
                        if (data != null) {
                            val instances = data.list
                            for (instance in instances) {
                                try {
                                    val e = provider.getEvent(instance.eventId)
                                    if (e != null && !e.deleted && instance.begin <= limit.timeInMillis && now.timeInMillis < instance.end && !CalendarHelper.getFilteredCalendarIdList()
                                            .contains(e.calendarId)
                                    ) {
                                        if (e.allDay) {
                                            val start = Calendar.getInstance()
                                            start.timeInMillis = instance.begin
                                            val end = Calendar.getInstance()
                                            end.timeInMillis = instance.end
                                            instance.begin =
                                                start.timeInMillis - start.timeZone.getOffset(start.timeInMillis)
                                            instance.end =
                                                end.timeInMillis - end.timeZone.getOffset(end.timeInMillis)
                                        }

                                        // Check all day events
                                        val startDate = Calendar.getInstance()
                                        startDate.timeInMillis = instance.begin
                                        val endDate = Calendar.getInstance()
                                        endDate.timeInMillis = instance.end

                                        val isAllDay = e.allDay || (
                                                startDate.get(Calendar.MILLISECOND) == 0
                                                        && startDate.get(Calendar.SECOND) == 0
                                                        && startDate.get(Calendar.MINUTE) == 0
                                                        && startDate.get(Calendar.HOUR_OF_DAY) == 0
                                                        && endDate.get(Calendar.MILLISECOND) == 0
                                                        && endDate.get(Calendar.SECOND) == 0
                                                        && endDate.get(Calendar.MINUTE) == 0
                                                        && endDate.get(Calendar.HOUR_OF_DAY) == 0
                                                )

                                        eventList.add(
                                            Event(
                                                id = instance.id,
                                                eventID = e.id,
                                                title = e.title ?: "",
                                                startDate = instance.begin,
                                                endDate = instance.end,
                                                calendarID = e.calendarId.toInt(),
                                                allDay = isAllDay,
                                                address = e.eventLocation ?: "",
                                                selfAttendeeStatus = e.selfAttendeeStatus.toInt(),
                                                availability = e.availability
                                            )
                                        )
                                    }
                                } catch (ignored: Exception) {
                                }
                            }
                        }

                        val sortedEvents = eventList.sortEvents()
                        val filteredEventList = sortedEvents
                            .applyFilters()

                        if (filteredEventList.isEmpty()) {
                            eventRepository.resetNextEventData()
                            eventRepository.clearEvents()
                        } else {
                            eventRepository.saveEvents(
                                sortedEvents
                            )
                            eventRepository.saveNextEventData(filteredEventList.first())
                        }
                    } catch (ignored: java.lang.Exception) {

                    }
                }
            } else {
                eventRepository.resetNextEventData()
            }

            UpdatesReceiver.setUpdates(this@UpdateCalendarService)
            MainWidget.updateWidget(this@UpdateCalendarService)

            EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
            eventRepository.close()

            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        job = null
    }

    private fun getCalendarSyncNotification(): Notification {
        with(NotificationManagerCompat.from(this)) {
            // Create channel
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                createNotificationChannel(
                    NotificationChannel(
                        getString(R.string.calendar_sync_notification_channel_id),
                        getString(R.string.calendar_sync_notification_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = getString(R.string.calendar_sync_notification_channel_description)
                    }
                )
            }

            val builder = NotificationCompat.Builder(this@UpdateCalendarService, getString(R.string.calendar_sync_notification_channel_id))
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(getString(R.string.calendar_sync_notification_title))
                .setOngoing(true)
                .setColor(ContextCompat.getColor(this@UpdateCalendarService, R.color.colorAccent))

            // Main intent that open the activity
            builder.setContentIntent(PendingIntent.getActivity(this@UpdateCalendarService, 0, Intent(this@UpdateCalendarService, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))

            return builder.build()
        }
    }
}