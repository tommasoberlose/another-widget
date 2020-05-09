package com.tommasoberlose.anotherwidget.models

class GlanceProvider(
    val id: String,
    val title: String,
    val icon: Int,
    val label: String = "",
    val enabled: Boolean = false,
    val isPermissionRequired: Boolean = false,
    val isPermissionGranted: (() -> Boolean)? = null,
    val requestPermission: (() -> Unit)? = null
)