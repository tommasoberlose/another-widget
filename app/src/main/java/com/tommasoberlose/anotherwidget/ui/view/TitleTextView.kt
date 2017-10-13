package com.tommasoberlose.anotherwidget.ui.view

import android.content.Context
import android.graphics.Typeface
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.Log
import android.widget.TextView
import com.tommasoberlose.anotherwidget.R


/**
 * Created by tommaso on 12/10/17.
 */
class TitleTextView : TextView {

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context) : super(context) {}

    fun init(context: Context, attrs: AttributeSet) {
        try {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.FontText)

            if (ta != null) {
                val fontAsset = ta.getString(R.styleable.FontText_typefaceAsset)

                if (fontAsset != null && !fontAsset.isEmpty()) {
                    val tf = Typeface.createFromAsset(getContext().assets, fontAsset)

                    if (tf != null)
                        typeface = tf
                    else
                        Log.i("FontText", String.format("Could not create a font from asset: %s", fontAsset))
                }
                ta.recycle()
            }
        } catch (e: Exception) {
            Log.i("FontText", "Could not create a font from asset")
        }

    }
}