package com.tommasoberlose.anotherwidget.models

import android.provider.CalendarContract
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by tommaso on 05/10/17.
 */

@Entity(tableName = "events")
data class Event(
    @PrimaryKey
    val id: Long = 0,
    @ColumnInfo(name = "event_id")
    val eventID: Long = 0,
    val title: String = "",
    @ColumnInfo(name = "start_date")
    val startDate: Long = 0,
    @ColumnInfo(name = "end_date")
    val endDate: Long = 0,
    @ColumnInfo(name = "calendar_id")
    val calendarID: Long = 0,
    @ColumnInfo(name = "all_day")
    val allDay: Boolean = false,
    val address: String = "",
    @ColumnInfo(name = "self_attendee_status")
    val selfAttendeeStatus: Int = CalendarContract.Attendees.ATTENDEE_STATUS_NONE,
    val availability: Int = CalendarContract.EventsEntity.AVAILABILITY_BUSY
)/* {
    override fun toString(): String {
        return "Event:\nEVENT ID: " + eventID + "\nTITLE: " + title + "\nSTART DATE: " + Date(startDate) + "\nEND DATE: " + Date(endDate) + "\nCAL ID: " + calendarID  + "\nADDRESS: " + address
    }
}*/
