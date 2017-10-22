package com.tommasoberlose.anotherwidget.`object`

import android.database.Cursor

import java.util.Date

/**
 * Created by tommaso on 05/10/17.
 */

class Event {
    var id: Int = 0
    var title: String = ""
    var startDate: Long = 0
    var endDate: Long = 0
    var calendarID: Int = 0
    var allDay: Boolean = false
    var address: String = ""

    constructor(id:Int, title:String, startDate:Long, endDate:Long, calendarID: Int, allDay: Boolean, address: String) {
        this.id = id
        this.title = title
        this.startDate = startDate
        this.endDate = endDate
        this.calendarID = calendarID
        this.allDay = allDay
        this.address = address
    }

    constructor(eventCursor: Cursor, instanceCursor: Cursor) {
        id = instanceCursor.getInt(0)
        startDate = instanceCursor.getLong(1)
        endDate = instanceCursor.getLong(2)

        title = eventCursor.getString(0)?: ""
        allDay = !eventCursor.getString(1).equals("0")
        calendarID = eventCursor.getInt(2)
        address = eventCursor.getString(3)?: ""
    }

    override fun toString(): String {
        return "Event:\nID" + id + "\nTITLE: " + title + "\nSTART DATE: " + Date(startDate) + "\nEND DATE: " + Date(endDate)
    }
}
