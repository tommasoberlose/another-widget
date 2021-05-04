package com.tommasoberlose.anotherwidget.utils

import android.animation.*
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Toast
import android.app.Activity
import android.app.WallpaperManager
import android.content.*
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import android.content.res.Configuration
import android.provider.Settings
import android.util.Patterns
import java.security.NoSuchAlgorithmException
import kotlin.math.max
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.animation.AlphaAnimation
import android.widget.RelativeLayout
import androidx.annotation.UiThread
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.OnSingleClickListener
import java.util.*


fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)

fun Context.toast(message: String, long: Boolean = false) {
    val toast = Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
//    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}

fun Int.toPixel(context: Context): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics).toInt()
fun Float.toPixel(context: Context): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics)

fun View.reveal(initialX: Int? = null, initialY: Int? = null) {

    when (visibility) {
        View.VISIBLE -> {
            val anim = ViewAnimationUtils.createCircularReveal(this, initialX ?: this.measuredWidth / 2, initialY ?: this.measuredHeight / 2, max(width.toFloat(), height.toFloat()), 0f)
                .apply {
                    duration = 200
                }
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                    super.onAnimationEnd(animation)
                }
            })
            anim.start()
        } else -> {
            val anim = ViewAnimationUtils.createCircularReveal(this, initialX ?: this.measuredWidth / 2, initialY ?: this.measuredHeight / 2, 0f, max(width.toFloat(), height.toFloat()))
                .apply {
                    duration = 200
                }
            visibility = View.VISIBLE
            anim.start()
        }
    }
}


fun View.expand(duration: Long = 500L) {
    clearAnimation()
    try {
        val animator = (tag as ValueAnimator)
        animator.removeAllListeners()
        animator.cancel()
    } catch (ex: java.lang.Exception) {}

    layoutParams = layoutParams.apply {
        height = RelativeLayout.LayoutParams.WRAP_CONTENT
    }
    measure(0, 0)

    val initialHeight = measuredHeight
    val anim = ValueAnimator.ofFloat(
        alpha,
        1f
    ).apply {
        this.duration = duration
        addUpdateListener {
            val animatedValue = animatedValue as Float
            layoutParams = layoutParams.apply {
                height = (initialHeight * animatedValue).toInt()
            }
            translationY = (initialHeight * animatedValue - initialHeight)
            alpha = animatedValue
        }
        addListener(
            onStart = {
                isVisible = true
            }
        )
    }
    tag = anim
    anim.start()
}

fun View.collapse(duration: Long = 500L) {
    clearAnimation()
    try {
        val animator = (tag as ValueAnimator)
        animator.removeAllListeners()
        animator.cancel()
    } catch (ex: java.lang.Exception) {}
    val initialHeight = measuredHeight
    val anim = ValueAnimator.ofFloat(
        alpha,
        0f
    ).apply {
        this.duration = duration
        addUpdateListener {
            val animatedValue = animatedValue as Float
            layoutParams = layoutParams.apply {
                height = (initialHeight * animatedValue).toInt()
            }
            translationY = (initialHeight * animatedValue - initialHeight)
            alpha = animatedValue
        }
        addListener(
            onEnd = {
                isVisible = false
            }
        )
    }
    tag = anim
    anim.start()
}

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

fun Context.isTablet(): Boolean {
    return (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
}

fun String.md5(): String {
    val mD5 = "MD5"
    try {
        // Create MD5 Hash
        val digest = java.security.MessageDigest
            .getInstance(mD5)
        digest.update(toByteArray())
        val messageDigest = digest.digest()

        // Create Hex String
        val hexString = StringBuilder()
        for (aMessageDigest in messageDigest) {
            var h = Integer.toHexString(0xFF and aMessageDigest.toInt())
            while (h.length < 2)
                h = "0$h"
            hexString.append(h)
        }
        return hexString.toString()

    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    }

    return ""
}

fun String.isValidEmail(): Boolean
        = this.isNotEmpty() &&
        Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun Context.isDarkTheme(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun Activity.isNotificationAccessGranted(): Boolean = Settings.Secure.getString(this.contentResolver,"enabled_notification_listeners").contains(this.packageName)

fun Float.convertDpToPixel(context: Context): Float {
    val resources: Resources = context.resources
    val metrics: DisplayMetrics = resources.displayMetrics
    val px: Float = this * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
    return px
}

fun Float.convertSpToPixels(context: Context): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this, context.resources.displayMetrics)
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
