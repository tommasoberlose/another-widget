package com.tommasoberlose.anotherwidget.ui.activities

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.tabs.TabLayoutMediator
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.MaterialBottomSheetDialog
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.helpers.BitmapHelper
import com.tommasoberlose.anotherwidget.helpers.ColorHelper
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.isColorDark
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.ui.adapters.ViewPagerAdapter
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.getCurrentWallpaper
import com.tommasoberlose.anotherwidget.utils.toPixel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.the_widget_sans.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var mAppWidgetId: Int = -1
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        controlExtras(intent)

        // Viewpager
        pager.adapter = ViewPagerAdapter(this)
        pager.offscreenPageLimit = 4
        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.settings_general_title)
                1 -> getString(R.string.settings_calendar_title)
                2 -> getString(R.string.settings_weather_title)
                3 -> getString(R.string.settings_clock_title)
                4 -> getString(R.string.advanced_settings_title)
                else -> ""
            }
        }.attach()

        // Init clock
        time.setTextColor(ColorHelper.getFontColor())
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, Preferences.clockTextSize.toPixel(this@MainActivity))
        time_am_pm.setTextColor(ColorHelper.getFontColor())
        time_am_pm.setTextSize(TypedValue.COMPLEX_UNIT_SP, Preferences.clockTextSize.toPixel(this@MainActivity) / 5 * 2)
        time_container.isVisible = Preferences.showClock

        preview.layoutParams = preview.layoutParams.apply {
            height = 160.toPixel(this@MainActivity) + if (Preferences.showClock) 100.toPixel(this@MainActivity) else 0
        }

        Preferences.preferences.registerOnSharedPreferenceChangeListener(this)
        subscribeUi(viewModel)
        updateUI()

        // Warnings
        if (getString(R.string.xiaomi_manufacturer).equals(Build.MANUFACTURER, ignoreCase = true) && Preferences.showXiaomiWarning) {
            MaterialBottomSheetDialog(this, getString(R.string.xiaomi_warning_title), getString(R.string.xiaomi_warning_message))
                .setNegativeButton(getString(R.string.action_ignore)) {
                    Preferences.showXiaomiWarning = false
                }
                .setPositiveButton(getString(R.string.action_grant_permission)) {
                    Preferences.showXiaomiWarning = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
                .show()
        }
    }

    private var uiJob: Job? = null

    private fun updateUI() {
        uiJob?.cancel()

        if (Preferences.showPreview) {
            preview.setCardBackgroundColor(
                getColor(
                    if (ColorHelper.getFontColor()
                            .isColorDark()
                    ) android.R.color.white else R.color.colorAccent
                )
            )
            widget_shape_background.setImageDrawable(BitmapHelper.getTintedDrawable(this, R.drawable.card_background, ColorHelper.getBackgroundColor()))
            uiJob = lifecycleScope.launch(Dispatchers.IO) {
                delay(200)
                val generatedView = MainWidget.generateWidgetView(this@MainActivity)

                withContext(Dispatchers.Main) {
                    generatedView.measure(0, 0)
                    preview.measure(0, 0)
                }

                val bitmap = BitmapHelper.getBitmapFromView(
                    generatedView,
                    if (preview.width > 0) preview.width else generatedView.measuredWidth,
                    generatedView.measuredHeight
                )
                withContext(Dispatchers.Main) {
                    // Clock
                    time.setTextColor(ColorHelper.getFontColor())
                    time_am_pm.setTextColor(ColorHelper.getFontColor())
                    time.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        Preferences.clockTextSize.toPixel(this@MainActivity)
                    )
                    time_am_pm.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        Preferences.clockTextSize.toPixel(this@MainActivity) / 5 * 2
                    )

                    // Clock bottom margin
                    clock_bottom_margin_none.isVisible =
                        Preferences.showClock && Preferences.clockBottomMargin == Constants.ClockBottomMargin.NONE.value
                    clock_bottom_margin_small.isVisible =
                        Preferences.showClock && Preferences.clockBottomMargin == Constants.ClockBottomMargin.SMALL.value
                    clock_bottom_margin_medium.isVisible =
                        Preferences.showClock && Preferences.clockBottomMargin == Constants.ClockBottomMargin.MEDIUM.value
                    clock_bottom_margin_large.isVisible =
                        Preferences.showClock && Preferences.clockBottomMargin == Constants.ClockBottomMargin.LARGE.value

                    if ((Preferences.showClock && !time_container.isVisible) || (!Preferences.showClock && time_container.isVisible)) {
                        if (Preferences.showClock) {
                            time_container.layoutParams = time_container.layoutParams.apply {
                                height = RelativeLayout.LayoutParams.WRAP_CONTENT
                            }
                            time_container.measure(0, 0)
                        }
                        val initialHeight = time_container.measuredHeight
                        ValueAnimator.ofFloat(
                            if (Preferences.showClock) 0f else 1f,
                            if (Preferences.showClock) 1f else 0f
                        ).apply {
                            duration = 500L
                            addUpdateListener {
                                val animatedValue = animatedValue as Float
                                time_container.layoutParams = time_container.layoutParams.apply {
                                    height = (initialHeight * animatedValue).toInt()
                                }
                            }
                            addListener(
                                onStart = {
                                    if (Preferences.showClock) {
                                        time_container.isVisible = true
                                    }
                                },
                                onEnd = {
                                    if (!Preferences.showClock) {
                                        time_container.isVisible = false
                                    }
                                }
                            )
                        }.start()

                        ValueAnimator.ofInt(
                            preview.height,
                            160.toPixel(this@MainActivity) + if (Preferences.showClock) 100.toPixel(
                                this@MainActivity
                            ) else 0
                        ).apply {
                            duration = 500L
                            addUpdateListener {
                                val animatedValue = animatedValue as Int
                                val layoutParams = preview.layoutParams
                                layoutParams.height = animatedValue
                                preview.layoutParams = layoutParams
                            }
                        }.start()
                    } else {
                        time_container.layoutParams = time_container.layoutParams.apply {
                            height = RelativeLayout.LayoutParams.WRAP_CONTENT
                        }
                        time_container.measure(0, 0)
                    }

                    if (preview.height == 0) {
                        ValueAnimator.ofInt(
                            preview.height,
                            160.toPixel(this@MainActivity) + if (Preferences.showClock) 100.toPixel(
                                this@MainActivity
                            ) else 0
                        ).apply {
                            duration = 300L
                            addUpdateListener {
                                val animatedValue = animatedValue as Int
                                val layoutParams = preview.layoutParams
                                layoutParams.height = animatedValue
                                preview.layoutParams = layoutParams
                            }
                        }.start()
                    }

                    bitmap_container.setImageBitmap(bitmap)
                    widget_loader.animate().scaleX(0f).scaleY(0f).start()
                    widget.animate().alpha(1f).start()
                }
            }
        } else {
            ValueAnimator.ofInt(
                preview.height,
                0
            ).apply {
                duration = 300L
                addUpdateListener {
                    val animatedValue = animatedValue as Int
                    val layoutParams = preview.layoutParams
                    layoutParams.height = animatedValue
                    preview.layoutParams = layoutParams
                }
            }.start()
        }


        // Calendar error indicator
        tabs.getTabAt(1)?.orCreateBadge?.apply {
            backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.errorColorText)
            badgeGravity = BadgeDrawable.TOP_END
        }?.isVisible = Preferences.showEvents && !checkGrantedPermission(Manifest.permission.READ_CALENDAR)

        // Weather error indicator
        tabs.getTabAt(2)?.orCreateBadge?.apply {
            backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.errorColorText)
            badgeGravity = BadgeDrawable.TOP_END
        }?.isVisible = Preferences.showWeather && (Preferences.weatherProviderApi == "" || (Preferences.customLocationAdd == "" && !checkGrantedPermission(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACCESS_BACKGROUND_LOCATION else Manifest.permission.ACCESS_FINE_LOCATION)))

    }

    private fun subscribeUi(viewModel: MainViewModel) {
        viewModel.showWallpaper.observe(this, Observer {
            val wallpaper = getCurrentWallpaper()
            widget_bg.setImageDrawable(if (it) wallpaper else null)
            widget_bg.layoutParams = widget_bg.layoutParams.apply {

                val metrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(metrics)

                height = metrics.heightPixels
                width = (wallpaper?.intrinsicWidth ?: 1) * metrics.heightPixels / (wallpaper?.intrinsicWidth ?: 1)
            }
        })

        logo.setOnClickListener {
//            startActivity(Intent(this, SupportDevActivity::class.java))
        }
    }

    override fun onBackPressed() {
        if (mAppWidgetId > 0) {
            addNewWidget()
        } else {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null) {
            controlExtras(intent)
        }
    }

    private fun controlExtras(intent: Intent) {
        val extras = intent.extras
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID)

            if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                action_add_widget.visibility = View.VISIBLE
                action_add_widget.setOnClickListener {
                    addNewWidget()
                }
            }


            if (extras.containsKey(Actions.ACTION_EXTRA_OPEN_WEATHER_PROVIDER)) {
                startActivityForResult(Intent(this, WeatherProviderActivity::class.java), RequestCode.WEATHER_PROVIDER_REQUEST_CODE.code)
            }
        }
    }

    private fun addNewWidget() {
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Preferences.preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, p1: String) {
        updateUI()
        MainWidget.updateWidget(this)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    class UpdateUiMessageEvent

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(ignore: UpdateUiMessageEvent?) {
        updateUI()
    }
}
