package com.tommasoberlose.anotherwidget.utils

import android.content.pm.PackageManager
import android.view.View
import android.widget.Toast
import android.app.WallpaperManager
import android.content.*
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import android.content.res.Configuration
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.browser.customtabs.CustomTabColorSchemeParams
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.OnSingleClickListener
import java.util.*


fun Context.toast(message: String, long: Boolean = false) {
    val toast = Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
//    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}

fun Int.toPixel(context: Context): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics).toInt()
fun Float.toPixel(context: Context): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics)


fun Context.openURI(url: String) {
    try {
        val builder: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
        builder.setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary)).build())
        val customTabsIntent: CustomTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
    } catch (e: Exception) {
        try {
            val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(openIntent)
        } catch (ignored: Exception) {
            val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(getString(R.string.app_name), url)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.error_opening_uri, Toast.LENGTH_LONG).show()
        }
    }
}

fun Context.isDarkTheme(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun Float.convertDpToPixel(context: Context): Float {
    val resources: Resources = context.resources
    val metrics: DisplayMetrics = resources.displayMetrics
    return this * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
}

fun Context.checkGrantedPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

fun Context.getCurrentWallpaper(): Drawable? = try {
    WallpaperManager.getInstance(this).drawable
} catch (e: Exception) {
    null
}

fun String.getCapWordString(): String {
    return try {
        val ar = this.split(" ")
        var newText = ""
        for (t: String in ar) {
            newText += " "
            newText += t.substring(0, 1).toUpperCase(Locale.getDefault())
            newText += t.substring(1)
        }
        newText.substring(1)
    } catch (e: Exception) {
        this
    }
}

fun Context.checkIfFitInstalled(): Boolean {
    return try {
        packageManager.getPackageInfo("com.google.android.apps.fitness", PackageManager.GET_ACTIVITIES)
        true
    } catch (e: Exception) {
        false
    }
}

fun Intent.isDefaultSet(context: Context): Boolean {
    val pm = context.packageManager
    return try {
        resolveActivity(pm) != null && resolveActivity(pm).packageName.isNotBlank()
    } catch (ex: java.lang.Exception) {
        false
    }
}

fun Locale.isMetric(): Boolean {
    return when (country.toUpperCase(this)) {
        "US", "LR", "MM", "GB" -> false
        else -> true
    }
}

fun View.setOnSingleClickListener(l: View.OnClickListener) {
    setOnClickListener(OnSingleClickListener(l))
}

fun View.setOnSingleClickListener(l: (View) -> Unit) {
    setOnClickListener(OnSingleClickListener(l))
}

fun ignoreExceptions(function: () -> Unit) = run {
    try {
        function.invoke()
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}
