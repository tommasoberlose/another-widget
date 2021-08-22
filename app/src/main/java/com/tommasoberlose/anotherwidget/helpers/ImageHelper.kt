package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import android.graphics.*
import android.renderscript.*
import android.util.TypedValue
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import com.tommasoberlose.anotherwidget.utils.isDarkTheme
import kotlin.math.min

object ImageHelper {
    fun ImageView.applyShadow(originalView: ImageView, factor: Float = 1f) {
        clearColorFilter()
        val cElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, when (if (context.isDarkTheme()) com.tommasoberlose.anotherwidget.global.Preferences.textShadowDark else com.tommasoberlose.anotherwidget.global.Preferences.textShadow) {
            0 -> 0f * factor
            1 -> 8f * factor
            2 -> 16f * factor
            else -> 0f * factor
        }, resources.displayMetrics)

        if (originalView.drawable != null && originalView.drawable.intrinsicWidth > 0 && originalView.drawable.intrinsicHeight > 0) {
            val btm = originalView.drawable.toBitmap().copy(Bitmap.Config.ARGB_8888, true)
            val comb = Bitmap.createBitmap(btm)
            val shadowBitmap = generateShadowBitmap(context, cElevation, btm, factor)

            shadowBitmap?.let {
                val canvas = Canvas(comb)
                canvas.drawColor(Color.TRANSPARENT)
                canvas.save()
                val rect = Rect()
//                val bounds = originalView.drawable.copyBounds()
                canvas.getClipBounds(rect)
                rect.inset(-2 * getBlurRadius(context, cElevation).toInt(), -2 * getBlurRadius(context, cElevation).toInt())
                canvas.save()
                canvas.clipRect(rect)
                canvas.drawBitmap(shadowBitmap, 0f, 2f, null)
                canvas.restore()
                setImageBitmap(comb)
            }
        }
    }

    private fun generateShadowBitmap(context: Context, cElevation: Float, bitmap: Bitmap?, factor: Float): Bitmap? {
        val rs: RenderScript = RenderScript.create(context)
        val element = Element.U8_4(rs)
        val blurScript: ScriptIntrinsicBlur = ScriptIntrinsicBlur.create(rs, element)
        val colorMatrixScript: ScriptIntrinsicColorMatrix = ScriptIntrinsicColorMatrix.create(rs)
        val allocationIn = Allocation.createFromBitmap(rs, bitmap)
        val allocationOut = Allocation.createTyped(rs, allocationIn.type)

        val matrix = Matrix4f(floatArrayOf(
            0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f,
            0f, 0f, 0f, when (if (context.isDarkTheme()) com.tommasoberlose.anotherwidget.global.Preferences.textShadowDark else com.tommasoberlose.anotherwidget.global.Preferences.textShadow) {
                0 -> 0f * factor
                1 -> 0.8f * factor
                2 -> 1f * factor
                else -> 0f
            }))

        colorMatrixScript.setColorMatrix(matrix)
        colorMatrixScript.forEach(allocationIn, allocationOut)

        blurScript.setRadius(getBlurRadius(context, cElevation))

        blurScript.setInput(allocationOut)
        blurScript.forEach(allocationIn)

        allocationIn.copyTo(bitmap)

        allocationIn.destroy()
        allocationOut.destroy()

        return bitmap
    }

    private fun getBlurRadius(context: Context, customElevation: Float): Float {
        val maxElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, context.resources.displayMetrics)
        return min(25f * (customElevation / maxElevation), 25f)
    }
}