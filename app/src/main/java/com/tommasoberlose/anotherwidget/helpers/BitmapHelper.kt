package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

object BitmapHelper {


    fun getBitmapFromView(view: View): Bitmap {
        //Define a bitmap with the same size as the view
        val measuredWidth = View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.UNSPECIFIED)
        val measuredHeight = View.MeasureSpec.makeMeasureSpec(view.height, View.MeasureSpec.UNSPECIFIED)
        view.measure(measuredWidth, measuredHeight)
        view.layout(0,0, measuredWidth, measuredHeight)
        val returnedBitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    fun getBitmapFromView(view: View, w: Int, h: Int): Bitmap {
        //Define a bitmap with the same size as the view
        val measuredWidth = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY)
        val measuredHeight = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        view.measure(measuredWidth, measuredHeight)
        view.layout(0,0, measuredWidth, measuredHeight)
        val returnedBitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }

        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    fun getTintedDrawable(context: Context, inputDrawable: Int, color: Int): Drawable?  = ContextCompat.getDrawable(context, inputDrawable)?.apply {
        DrawableCompat.setTint(this, color)
        DrawableCompat.setTintMode(this, PorterDuff.Mode.SRC_IN)
    }

    fun changeBitmapColor(sourceBitmap: Bitmap, color: Int): Bitmap {
        val resultBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0,
            sourceBitmap.width - 1, sourceBitmap.height - 1)
        val p = Paint()
        val filter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        p.colorFilter = filter

        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(resultBitmap, 0f, 0f, p)

        return resultBitmap
    }
}