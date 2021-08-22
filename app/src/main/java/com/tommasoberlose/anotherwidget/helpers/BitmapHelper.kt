package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

object BitmapHelper {

    fun getBitmapFromView(view: View, width: Int? = null, height: Int? = null, draw: Boolean = true): Bitmap {
        //Define a bitmap with the same size as the view
        val measuredWidth = View.MeasureSpec.makeMeasureSpec(width ?: view.width, if (width != null) View.MeasureSpec.EXACTLY else View.MeasureSpec.AT_MOST)
        val measuredHeight = View.MeasureSpec.makeMeasureSpec(height ?: view.height, if (height != null) View.MeasureSpec.EXACTLY else View.MeasureSpec.UNSPECIFIED)
        view.measure(
            if (measuredWidth > 0) measuredWidth else 0,
            if (measuredHeight > 0) measuredHeight else 0
        )

        val calculatedWidth = view.measuredWidth
        val widgetWidth = if (calculatedWidth in 1..16000) {
            calculatedWidth
        } else if (width != null && width > 0) {
            width
        } else {
            1
        }
        val calculatedHeight = view.measuredHeight
        val widgetHeight = if (calculatedHeight in 1..16000) {
            calculatedHeight
        } else if (height != null && height > 0) {
            height
        } else {
            1
        }

        return try {
            val btm = Bitmap.createBitmap(
                widgetWidth,
                widgetHeight,
                if (draw) Bitmap.Config.ARGB_8888 else Bitmap.Config.ALPHA_8
            )
            if (draw) {
                //Bind a canvas to it
                val canvas = Canvas(btm)
                // draw the view on the canvas
                view.layout(0, 0, measuredWidth, measuredHeight)
                view.draw(canvas)
                //return the bitmap
            }
            btm
        } catch (ex: Exception) {
            Bitmap.createBitmap(5, 5, Bitmap.Config.ALPHA_8)
        }
    }

    fun getTintedDrawable(context: Context, inputDrawable: Int, color: Int): Drawable?  = ContextCompat.getDrawable(context, inputDrawable)?.apply {
        DrawableCompat.setTint(this, color)
        DrawableCompat.setTintMode(this, PorterDuff.Mode.SRC_IN)
    }
}