package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.drawToBitmap
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.lang.Exception

object BitmapHelper {

    fun getBitmapFromView(view: View, width: Int? = null, height: Int? = null, draw: Boolean = true): Bitmap {
        //Define a bitmap with the same size as the view
        val measuredWidth = View.MeasureSpec.makeMeasureSpec(width ?: view.width, if (width != null) View.MeasureSpec.EXACTLY else View.MeasureSpec.UNSPECIFIED)
        val measuredHeight = View.MeasureSpec.makeMeasureSpec(height ?: view.height, if (height != null) View.MeasureSpec.EXACTLY else View.MeasureSpec.UNSPECIFIED)
        view.measure(measuredWidth, measuredHeight)

        return try {
            Log.d("ciao", "bitmap ${view.measuredWidth}, ${view.measuredHeight} - draw = ${draw}")
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
}