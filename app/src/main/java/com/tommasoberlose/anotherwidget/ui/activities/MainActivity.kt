package com.tommasoberlose.anotherwidget.ui.activities

import android.animation.ValueAnimator
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.helpers.BitmapHelper
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper
import com.tommasoberlose.anotherwidget.helpers.ColorHelper
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.isColorDark
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.ui.adapters.ViewPagerAdapter
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.getCurrentWallpaper
import com.tommasoberlose.anotherwidget.utils.toPixel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        clock.setTextColor(ColorHelper.getFontColor())
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, Preferences.clockTextSize.toPixel(this@MainActivity))
        clock.format12Hour = "hh:mm"
        clock.isVisible = Preferences.showClock

        preview.layoutParams = preview.layoutParams.apply {
            height = 160.toPixel(this@MainActivity) + if (Preferences.showClock) 100.toPixel(this@MainActivity) else 0
        }

        Preferences.preferences.registerOnSharedPreferenceChangeListener(this)
        subscribeUi(viewModel)
        updateUI()

        CalendarHelper.updateEventList(this)
        WeatherHelper.updateWeather(this)
    }

    private var uiJob: Job? = null

    private fun updateUI() {
        preview.setCardBackgroundColor(getColor(if (ColorHelper.getFontColor().isColorDark()) android.R.color.white else R.color.colorAccent))
        val generatedView = MainWidget.generateWidgetView(this@MainActivity)
        generatedView.measure(0, 0)
        preview.measure(0, 0)

        uiJob?.cancel()
        uiJob = lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = BitmapHelper.getBitmapFromView(generatedView, if (preview.width > 0) preview.width else generatedView.measuredWidth, generatedView.measuredHeight)
            withContext(Dispatchers.Main) {
                // Clock
                clock.setTextColor(ColorHelper.getFontColor())
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
                        preview.height,
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
                widget_loader.animate().scaleX(0f).scaleY(0f).start()
                widget.animate().alpha(1f).start()
            }
        }
    }

    private fun subscribeUi(viewModel: MainViewModel) {
        viewModel.showWallpaper.observe(this, Observer {
            widget_bg.setImageDrawable(if (it) getCurrentWallpaper() else null)
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
