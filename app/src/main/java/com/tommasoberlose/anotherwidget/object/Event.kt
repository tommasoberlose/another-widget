package com.tommasoberlose.anotherwidget.`object`

import android.database.Cursor

import java.util.Date

/**
 * Created by tommaso on 05/10/17.
 */

class Event {
    var id: Int = 0
    var title: String? = null
    var startDate: Long = 0
    var endDate: Long = 0
    var calendarID: Int = 0

    constructor(id:Int, title:String, startDate:Long, endDate:Long, calendarID: Int) {
        this.id = id
        this.title = title
        this.startDate = startDate
        this.endDate = endDate
        this.calendarID = calendarID
    }

    constructor(eventCursor: Cursor, instanceCursor: Cursor) {
        id = instanceCursor.getInt(0)
        startDate = instanceCursor.getLong(1)
        endDate = instanceCursor.getLong(2)

        title = eventCursor.getString(0)
        calendarID = eventCursor.getInt(2)
    }

    override fun toString(): String {
        return "Event:\nID" + id + "\nTITLE: " + title + "\nSTART DATE: " + Date(startDate) + "\nEND DATE: " + Date(endDate)
    }
}
