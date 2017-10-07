package com.tommasoberlose.anotherwidget.ui.activity

import android.Manifest
import android.os.Bundle
import com.tommasoberlose.anotherwidget.R
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide


class SplashActivity : IntroActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isButtonNextVisible = true
        isButtonBackVisible = false
        isButtonCtaVisible = false
        buttonCtaTintMode = BUTTON_CTA_TINT_MODE_TEXT

        addSlide(SimpleSlide.Builder()
                .title(R.string.app_name)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .build())

        addSlide(SimpleSlide.Builder()
                .title(R.string.title_permission_calendar)
                .description(R.string.description_permission_calendar)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .permission(Manifest.permission.READ_CALENDAR)
                .build())

        addSlide(SimpleSlide.Builder()
                .title(R.string.title_permission_location)
                .description(R.string.description_permission_location)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .permission(Manifest.permission.ACCESS_COARSE_LOCATION)
                .build())
    }
}
