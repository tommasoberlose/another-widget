package com.tommasoberlose.anotherwidget.ui.fragments

import android.Manifest
import android.animation.ValueAnimator
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.animation.addListener
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.MaterialSharedAxis
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.MaterialBottomSheetDialog
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.BitmapHelper
import com.tommasoberlose.anotherwidget.helpers.ColorHelper
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.isColorDark
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.adapters.ViewPagerAdapter
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.getCurrentWallpaper
import com.tommasoberlose.anotherwidget.utils.toPixel
import kotlinx.android.synthetic.main.fragment_app_main.*
import kotlinx.android.synthetic.main.the_widget_sans.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainFragment  : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        fun newInstance() = MainFragment()
        private const val PREVIEW_BASE_HEIGHT = 120
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        return inflater.inflate(R.layout.fragment_app_main, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        subscribeUi(viewModel)

        // Viewpager
        pager.adapter = ViewPagerAdapter(requireActivity())
        pager.offscreenPageLimit = 4
        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.settings_general_title)
                1 -> getString(R.string.settings_calendar_title)
                2 -> getString(R.string.settings_weather_title)
                3 -> getString(R.string.settings_clock_title)
                4 -> getString(R.string.settings_at_a_glance_title)
                else -> ""
            }
        }.attach()

        // Init clock
        time.setTextColor(ColorHelper.getClockFontColor())
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, Preferences.clockTextSize.toPixel(requireContext()))
        time_am_pm.setTextColor(ColorHelper.getClockFontColor())
        time_am_pm.setTextSize(TypedValue.COMPLEX_UNIT_SP, Preferences.clockTextSize.toPixel(requireContext()) / 5 * 2)
        time_container.isVisible = Preferences.showClock

        preview.layoutParams = preview.layoutParams.apply {
            height = PREVIEW_BASE_HEIGHT.toPixel(requireContext()) + if (Preferences.showClock) 100.toPixel(requireContext()) else 0
        }
        subscribeUi(viewModel)
        updateUI()

        // Warnings
        if (getString(R.string.xiaomi_manufacturer).equals(Build.MANUFACTURER, ignoreCase = true) && Preferences.showXiaomiWarning) {
            MaterialBottomSheetDialog(requireContext(), getString(R.string.xiaomi_warning_title), getString(R.string.xiaomi_warning_message))
                .setNegativeButton(getString(R.string.action_ignore)) {
                    Preferences.showXiaomiWarning = false
                }
                .setPositiveButton(getString(R.string.action_grant_permission)) {
                    Preferences.showXiaomiWarning = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${activity?.packageName}")
                    }
                    startActivity(intent)
                }
                .show()
        }
    }

    private var uiJob: Job? = null

    private fun updateUI() {
        uiJob?.cancel()

        preview?.clearAnimation()
        time_container?.clearAnimation()

        if (Preferences.showPreview) {
            preview?.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (ColorHelper.getFontColor()
                            .isColorDark()
                    ) android.R.color.white else R.color.colorAccent
                )
            )
            widget_shape_background?.setImageDrawable(
                BitmapHelper.getTintedDrawable(
                    requireContext(),
                    R.drawable.card_background,
                    ColorHelper.getBackgroundColor()
                )
            )
            uiJob = lifecycleScope.launch(Dispatchers.IO) {
                val generatedView = MainWidget.generateWidgetView(requireContext())

                withContext(Dispatchers.Main) {
                    generatedView.measure(0, 0)
                    preview?.measure(0, 0)
                }

                val bitmap = if (preview != null) {
                    BitmapHelper.getBitmapFromView(
                        generatedView,
                        if (preview.width > 0) preview.width else generatedView.measuredWidth,
                        generatedView.measuredHeight
                    )
                } else {
                    null
                }
                withContext(Dispatchers.Main) {
                    // Clock
                    time?.setTextColor(ColorHelper.getClockFontColor())
                    time_am_pm?.setTextColor(ColorHelper.getClockFontColor())
                    time?.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        Preferences.clockTextSize.toPixel(requireContext())
                    )
                    time_am_pm?.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        Preferences.clockTextSize.toPixel(requireContext()) / 5 * 2
                    )
                    time_am_pm?.isVisible = Preferences.showAMPMIndicator

                    // Clock bottom margin
                    clock_bottom_margin_none?.isVisible =
                        Preferences.showClock && Preferences.clockBottomMargin == Constants.ClockBottomMargin.NONE.value
                    clock_bottom_margin_small?.isVisible =
                        Preferences.showClock && Preferences.clockBottomMargin == Constants.ClockBottomMargin.SMALL.value
                    clock_bottom_margin_medium?.isVisible =
                        Preferences.showClock && Preferences.clockBottomMargin == Constants.ClockBottomMargin.MEDIUM.value
                    clock_bottom_margin_large?.isVisible =
                        Preferences.showClock && Preferences.clockBottomMargin == Constants.ClockBottomMargin.LARGE.value

                    if ((Preferences.showClock && (time?.alpha ?: 1f < 1f)) || (!Preferences.showClock && (time?.alpha ?: 0f > 0f))) {
                        if (Preferences.showClock) {
                            time_container?.layoutParams = time_container.layoutParams.apply {
                                height = RelativeLayout.LayoutParams.WRAP_CONTENT
                            }
                            time_container?.measure(0, 0)
                        }
                        val initialHeight = time_container?.measuredHeight ?: 0
                        ValueAnimator.ofFloat(
                            if (Preferences.showClock) 0f else 1f,
                            if (Preferences.showClock) 1f else 0f
                        ).apply {
                            duration = 500L
                            addUpdateListener {
                                val animatedValue = animatedValue as Float
                                time_container?.layoutParams =
                                    time_container.layoutParams.apply {
                                        height = (initialHeight * animatedValue).toInt()
                                    }
                                time?.alpha = animatedValue
                            }
                            addListener(
                                onStart = {
                                    if (Preferences.showClock) {
                                        time_container?.isVisible = true
                                    }
                                },
                                onEnd = {
                                    if (!Preferences.showClock) {
                                        time_container?.isVisible = false
                                    }
                                }
                            )
                        }.start()

                        if (preview != null) {
                            ValueAnimator.ofInt(
                                preview.height,
                                PREVIEW_BASE_HEIGHT.toPixel(requireContext()) + if (Preferences.showClock) 100.toPixel(
                                    requireContext()
                                ) else 0
                            ).apply {
                                duration = 500L
                                addUpdateListener {
                                    if (preview != null) {
                                        val animatedValue = animatedValue as Int
                                        val layoutParams = preview.layoutParams
                                        layoutParams.height = animatedValue
                                        preview.layoutParams = layoutParams
                                    }
                                }
                            }.start()
                        }
                    } else {
                        time_container?.layoutParams = time_container.layoutParams.apply {
                            height = RelativeLayout.LayoutParams.WRAP_CONTENT
                        }
                        time_container?.measure(0, 0)
                    }

                    if (preview != null && preview.height == 0) {
                        ValueAnimator.ofInt(
                            preview.height,
                            PREVIEW_BASE_HEIGHT.toPixel(requireContext()) + if (Preferences.showClock) 100.toPixel(
                                requireContext()
                            ) else 0
                        ).apply {
                            duration = 300L
                            addUpdateListener {
                                if (preview != null) {
                                    val animatedValue = animatedValue as Int
                                    val layoutParams = preview.layoutParams
                                    layoutParams.height = animatedValue
                                    preview?.layoutParams = layoutParams
                                }
                            }
                        }.start()
                    }

                    widget_loader?.animate()?.scaleX(0f)?.scaleY(0f)?.alpha(0f)?.setDuration(200L)?.start()
                    bitmap_container?.apply {
                        setImageBitmap(bitmap)
                        scaleX = 0.9f
                        scaleY = 0.9f
                    }
                    widget?.animate()?.alpha(1f)?.start()
                }
            }
        } else {
            if (preview != null) {
                ValueAnimator.ofInt(
                    preview.height,
                    0
                ).apply {
                    duration = 300L
                    addUpdateListener {
                        if (preview != null) {
                            val animatedValue = animatedValue as Int
                            val layoutParams = preview.layoutParams
                            layoutParams.height = animatedValue
                            preview.layoutParams = layoutParams
                        }
                    }
                }.start()
            }
        }

        showErrorBadge()

    }

    private fun subscribeUi(viewModel: MainViewModel) {
        viewModel.showWallpaper.observe(viewLifecycleOwner, Observer {
            activity?.let { act ->
                val wallpaper = act.getCurrentWallpaper()
                widget_bg.setImageDrawable(if (it) wallpaper else null)
                if (wallpaper != null) {
                    widget_bg.layoutParams =
                        (widget_bg.layoutParams as ViewGroup.MarginLayoutParams).apply {

                            val metrics = DisplayMetrics()
                            act.windowManager.defaultDisplay.getMetrics(metrics)

                            val dimensions: Pair<Int, Int> = if (wallpaper.intrinsicWidth >= wallpaper.intrinsicHeight) {
                                metrics.heightPixels to (wallpaper.intrinsicWidth) * metrics.heightPixels / (wallpaper.intrinsicHeight)
                            } else {
                                metrics.widthPixels to (wallpaper.intrinsicHeight) * metrics.widthPixels / (wallpaper.intrinsicWidth)
                            }

                            setMargins(
                                if (dimensions.first >= dimensions.second) (-80).toPixel(requireContext()) else 0,
                                (-80).toPixel(requireContext()), 0, 0
                            )

                            width = dimensions.first
                            height = dimensions.second
                        }
                }
            }
        })

        logo.setOnClickListener {
//            startActivity(Intent(this, SupportDevActivity::class.java))
        }

        action_settings.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_appMainFragment_to_appSettingsFragment)
        }
    }

    private fun showErrorBadge() {
        // Calendar error indicator
        tabs?.getTabAt(1)?.orCreateBadge?.apply {
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.errorColorText)
            badgeGravity = BadgeDrawable.TOP_END
        }?.isVisible = Preferences.showEvents && activity?.checkGrantedPermission(Manifest.permission.READ_CALENDAR) != true

        // Weather error indicator
        tabs?.getTabAt(2)?.orCreateBadge?.apply {
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.errorColorText)
            badgeGravity = BadgeDrawable.TOP_END
        }?.isVisible = Preferences.showWeather && (Preferences.weatherProviderApi == "" || (Preferences.customLocationAdd == "" && activity?.checkGrantedPermission(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACCESS_BACKGROUND_LOCATION else Manifest.permission.ACCESS_FINE_LOCATION) != true))


        // Music error indicator
        tabs?.getTabAt(4)?.orCreateBadge?.apply {
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.errorColorText)
            badgeGravity = BadgeDrawable.TOP_END
        }?.isVisible = Preferences.showMusic && !NotificationManagerCompat.getEnabledListenerPackages(requireContext()).contains(requireContext().packageName)
    }

    override fun onResume() {
        super.onResume()
        Preferences.preferences.registerOnSharedPreferenceChangeListener(this)
        EventBus.getDefault().register(this)
        showErrorBadge()
        updateUI()
    }

    override fun onPause() {
        Preferences.preferences.unregisterOnSharedPreferenceChangeListener(this)
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    var delayJob: Job? = null

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, p1: String) {
        delayJob?.cancel()
        delayJob = lifecycleScope.launch(Dispatchers.IO) {
            delay(200)
            withContext(Dispatchers.Main) {
                updateUI()
            }
        }
        MainWidget.updateWidget(requireContext())
    }

    class UpdateUiMessageEvent

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(ignore: UpdateUiMessageEvent?) {
        delayJob?.cancel()
        delayJob = lifecycleScope.launch(Dispatchers.IO) {
            delay(200)
            withContext(Dispatchers.Main) {
                updateUI()
            }
        }
    }
}
