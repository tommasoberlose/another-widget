package com.tommasoberlose.anotherwidget.models

/**
 * Created by tommaso on 08/10/17.
 */
class CalendarSelector(id: Long, name: String?, accountName: String?) {
    var id: Long = 0
    var name: String = ""
    var accountName: String = ""

    init {
        this.id = id
        this.name = name ?: ""
        this.accountName = accountName ?: ""
    }
}