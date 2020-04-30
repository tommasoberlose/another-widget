package com.tommasoberlose.anotherwidget.utils

import android.content.pm.PackageManager
import android.view.Gravity
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Toast
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
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
import android.util.TypedValue
import com.tommasoberlose.anotherwidget.R


fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)

fun Context.toast(message: String) {
    val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
//    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}

fun Int.toPixel(context: Context): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics).toInt()
fun Float.toPixel(context: Context): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics)

fun View.reveal(initialX: Int, initialY: Int) {
    when (visibility) {
        View.VISIBLE -> {
            val anim = ViewAnimationUtils.createCircularReveal(this, initialX, initialY, max(width.toFloat(), height.toFloat()), 0f)
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
            val anim = ViewAnimationUtils.createCircularReveal(this, initialX, initialY, 0f, max(width.toFloat(), height.toFloat()))
                .apply {
                    duration = 200
                }
            visibility = View.VISIBLE
            anim.start()
        }
    }
}

fun Context.openURI(url: String) {
    try {
        val builder: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
        builder.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
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
    val MD5 = "MD5"
    try {
        // Create MD5 Hash
        val digest = java.security.MessageDigest
            .getInstance(MD5)
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

fun Activity.isDarkTheme(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun Activity.isNotificationAccessGranted(): Boolean = Settings.Secure.getString(this.contentResolver,"enabled_notification_listeners").contains(this.packageName)

//fun Activity.sendEmailTo(email: String) {
//    val i = Intent(Intent.ACTION_VIEW).apply {
//        data = Uri.parse("mailto:$email")
//    }
//    try {
//        startActivity(Intent.createChooser(i, getString(R.string.settings_title_feedback)))
//    } catch (ex: java.lang.Exception) {
//        toast(getString(R.string.generic_error))
//    }
//
//}