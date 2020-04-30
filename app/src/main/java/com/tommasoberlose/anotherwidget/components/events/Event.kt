package com.tommasoberlose.anotherwidget.components.events

import io.realm.RealmObject
import java.util.Date

/**
 * Created by tommaso on 05/10/17.
 */

open class Event(var id: Long = 0,
                 var eventID: Long = 0,
            var title: String = "",
            var startDate: Long = 0,
            var endDate: Long = 0,
            var calendarID: Int = 0,
            var allDay: Boolean = false,
            var address: String = "") : RealmObject(){

    override fun toString(): String {
        return "Event:\nID: " + id + "\nTITLE: " + title + "\nSTART DATE: " + Date(startDate) + "\nEND DATE: " + Date(endDate) + "\nCAL DAY: " + calendarID  + "\nADDRESS: " + address
    }
}
