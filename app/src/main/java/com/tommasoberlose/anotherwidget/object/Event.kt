package com.tommasoberlose.anotherwidget.`object`

import android.database.Cursor

import java.util.Date

/**
 * Created by tommaso on 05/10/17.
 */

class Event(eventCursor: Cursor, instanceCursor: Cursor) {
    var id: Int = 0
    var title: String? = null
    var startDate: Long = 0
    var endDate: Long = 0

    init {
        id = instanceCursor.getInt(0)
        startDate = instanceCursor.getLong(1)
        endDate = instanceCursor.getLong(2)

        title = eventCursor.getString(0)
    }

    override fun toString(): String {
        return "Event:\nID" + id + "\nTITLE: " + title + "\nSTART DATE: " + Date(startDate) + "\nEND DATE: " + Date(endDate)
    }
}
