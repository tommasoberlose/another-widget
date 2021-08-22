package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView


class FixedFocusScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : NestedScrollView(context, attrs, defStyle) {

    var isScrollable = true

    override fun scrollTo(x: Int, y: Int) {
        if (isScrollable) {
            super.scrollTo(x, y)
        }
    }
}