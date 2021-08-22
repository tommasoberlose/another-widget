package com.tommasoberlose.anotherwidget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper

class NewCalendarEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventRepository = EventRepository(context)
        when (intent.action) {
            Intent.ACTION_PROVIDER_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                CalendarHelper.updateEventList(context)
            }
            Actions.ACTION_GO_TO_NEXT_EVENT -> {
                eventRepository.goToNextEvent()
            }
            Actions.ACTION_GO_TO_PREVIOUS_EVENT -> {
                eventRepository.goToPreviousEvent()
            }
        }
        eventRepository.close()
    }
}
