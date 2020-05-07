package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics


object BitmapHelper {

    fun getBitmapFromView(view: View, width: Int? = null, height: Int? = null, draw: Boolean = true): Bitmap {
        //Define a bitmap with the same size as the view
        val measuredWidth = View.MeasureSpec.makeMeasureSpec(width ?: view.width, if (width != null) View.MeasureSpec.EXACTLY else View.MeasureSpec.AT_MOST)
        val measuredHeight = View.MeasureSpec.makeMeasureSpec(height ?: view.height, if (height != null) View.MeasureSpec.EXACTLY else View.MeasureSpec.UNSPECIFIED)
        view.measure(measuredWidth, measuredHeight)

        if (draw) {
            FirebaseCrashlytics.getInstance().setCustomKey("initialWidth", width ?: -1)
            FirebaseCrashlytics.getInstance().setCustomKey("initialHeight", height ?: -1)
            FirebaseCrashlytics.getInstance().setCustomKey("measuredWidth", view.measuredWidth)
            FirebaseCrashlytics.getInstance().setCustomKey("measuredWidth_spec", measuredWidth)
            FirebaseCrashlytics.getInstance().setCustomKey("measuredHeight", view.measuredHeight)
            FirebaseCrashlytics.getInstance()
                .setCustomKey("measuredHeight_spec", measuredHeight)
        }

        return try {
            val btm = Bitmap.createBitmap(
                view.measuredWidth,
                view.measuredHeight,
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
            FirebaseCrashlytics.getInstance().recordException(ex)
            Bitmap.createBitmap(5, 5, Bitmap.Config.ALPHA_8)
        }
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

    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        var bitmap: Bitmap? = null
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }
        bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(
                1,
                1,
                Bitmap.Config.ARGB_8888
            ) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}