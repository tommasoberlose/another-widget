package com.tommasoberlose.anotherwidget.global

import java.text.SimpleDateFormat

object Constants {
    const val RESULT_CODE_CUSTOM_LOCATION = 45
    const val RESULT_APP_NAME = "RESULT_APP_NAME"
    const val RESULT_APP_PACKAGE = "RESULT_APP_PACKAGE"

    const val CUSTOM_FONT_PRODUCT_SANS = 1
    enum class ClockBottomMargin(val value: Int) {
        NONE(0),
        SMALL(1),
        MEDIUM(2),
        LARGE(3)
    }
}