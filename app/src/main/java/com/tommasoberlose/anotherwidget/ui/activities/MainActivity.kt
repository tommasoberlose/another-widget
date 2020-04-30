package com.tommasoberlose.anotherwidget.ui.activities

import android.animation.ValueAnimator
import android.app.Activity
import android.os.Bundle
import android.appwidget.AppWidgetManager
import android.view.View
import com.tommasoberlose.anotherwidget.R
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.ui.adapters.ViewPagerAdapter
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.receivers.WeatherReceiver
import com.tommasoberlose.anotherwidget.ui.widgets.TheWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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

        Preferences.preferences.registerOnSharedPreferenceChangeListener(this)
        subscribeUi(viewModel)
        updateUI()
    }

    private fun updateUI() {
        lifecycleScope.launch(Dispatchers.IO) {
            val generatedView = TheWidget.generateWidgetView(this@MainActivity, preview.measuredWidth)
            generatedView.measure(0, 0)
            val bitmap = Util.getBitmapFromView(generatedView, generatedView.measuredWidth, generatedView.measuredHeight)
            withContext(Dispatchers.Main) {
                // Clock
                clock.setTextColor(Util.getFontColor())
                clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, Preferences.clockTextSize.toPixel(this@MainActivity))
                clock.format12Hour = "hh:mm"

                if ((Preferences.showClock && !clock.isVisible) || (!Preferences.showClock && clock.isVisible)) {
                    if (Preferences.showClock) {
                        clock.layoutParams = clock.layoutParams.apply {
                            height = RelativeLayout.LayoutParams.WRAP_CONTENT
                        }
                        clock.measure(0, 0)
                    }
                    val initialHeight = clock.measuredHeight
                    ValueAnimator.ofFloat(
                        if (Preferences.showClock) 0f else 1f,
                        if (Preferences.showClock) 1f else 0f
                    ).apply {
                        duration = 500L
                        addUpdateListener {
                            val animatedValue = animatedValue as Float
                            clock.layoutParams = clock.layoutParams.apply {
                                height = (initialHeight * animatedValue).toInt()
                            }
                        }
                        addListener(
                            onStart = {
                                if (Preferences.showClock) {
                                    clock.isVisible = true
                                }
                            },
                            onEnd = {
                                if (!Preferences.showClock) {
                                    clock.isVisible = false
                                }
                            }
                        )
                    }.start()

                    ValueAnimator.ofInt(
                        preview.measuredHeight,
                        160.toPixel(this@MainActivity) + if (Preferences.showClock) 100.toPixel(this@MainActivity) else 0
                    ).apply {
                        duration = 500L
                        addUpdateListener {
                            val animatedValue = animatedValue as Int
                            val layoutParams = preview.layoutParams
                            layoutParams.height = animatedValue
                            preview.layoutParams = layoutParams
                        }
                    }.start()
                }

                widget_bitmap.setImageBitmap(bitmap)
            }
        }
    }

    private fun subscribeUi(viewModel: MainViewModel) {
        viewModel.showWallpaper.observe(this, Observer {
            widget_bg.setImageDrawable(if (it) Util.getCurrentWallpaper(this) else null)
        })
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
            if (extras.containsKey(Actions.ACTION_EXTRA_DISABLE_GPS_NOTIFICATION)) {
                Preferences.showGpsInformation = false
                sendBroadcast(Intent(Actions.ACTION_WEATHER_UPDATE))
                finish()
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
        Util.updateWidget(this)
    }
}
