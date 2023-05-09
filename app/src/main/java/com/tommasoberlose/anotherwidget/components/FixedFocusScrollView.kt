package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView


class FixedFocusScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : NestedScrollView(context, attrs, defStyle) {

    var isScrollable = true

    override fun scrollTo(x: Int, y: Int) {
        if (isScrollable || !isLaidOut) {
            super.scrollTo(x, y)
        }
    }
}
