package com.tommasoberlose.anotherwidget.`object`

import android.content.pm.ApplicationInfo

/**
 * Created by tommaso on 15/10/17.
 */

class ApplicationListEvent(apps: List<ApplicationInfo>, filtered: Boolean) {
    var apps: List<ApplicationInfo> = ArrayList()
    var filtered: Boolean = false
    init {
        this.apps = apps
        this.filtered = filtered
    }
}