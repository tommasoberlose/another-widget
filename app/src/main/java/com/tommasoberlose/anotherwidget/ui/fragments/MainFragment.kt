package com.tommasoberlose.anotherwidget.ui.fragments

import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.transition.MaterialSharedAxis
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.MaterialBottomSheetDialog
import com.tommasoberlose.anotherwidget.databinding.FragmentAppMainBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.*
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.isColorDark
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
        private val PREVIEW_BASE_HEIGHT: Int
            get() = if (Preferences.widgetAlign == Constants.WidgetAlign.CENTER.rawValue) 120 else 180
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var binding: FragmentAppMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        binding = FragmentAppMainBinding.inflate(inflater)

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

        val navHost = childFragmentManager.findFragmentById(R.id.settings_fragment) as? NavHostFragment?
        navHost?.navController?.addOnDestinationChangedListener { controller, destination, _ ->
            val show = destination.id != R.id.tabSelectorFragment
            binding.actionBack.animate().alpha(if (show) 1f else 0f).setDuration(200).translationX((if (show) 0f else 4f).convertDpToPixel(requireContext())).start()
            binding.actionBack.setOnSingleClickListener {
                controller.navigateUp()
            }
            binding.actionBack.isClickable = show
            binding.actionBack.isFocusable = show
            binding.actionSettings.animate().alpha(if (!show) 1f else 0f).setDuration(200).translationX((if (!show) 0f else -4f).convertDpToPixel(requireContext())).start()
            binding.actionSettings.isClickable = !show
            binding.actionSettings.isFocusable = !show
            binding.fragmentTitle.text = if (show) destination.label.toString() else getString(R.string.app_name)
        }

        binding.actionSettings.setOnSingleClickListener {
            Navigation.findNavController(it).navigate(R.id.action_appMainFragment_to_appSettingsFragment)
        }

        binding.preview.layoutParams = binding.preview.layoutParams.apply {
            height = PREVIEW_BASE_HEIGHT.toPixel(requireContext()) + if (Preferences.showClock) 100.toPixel(
                requireContext()
            ) else 0
        }

        subscribeUi(viewModel)

        return binding.root
    }

    private fun subscribeUi(viewModel: MainViewModel) {
        viewModel.showWallpaper.observe(viewLifecycleOwner) {
            if (it) {
                val wallpaper = requireActivity().getCurrentWallpaper()
                binding.widgetBg.setImageDrawable(if (it) wallpaper else null)
                if (wallpaper != null) {
                    binding.widgetBg.layoutParams =
                        (binding.widgetBg.layoutParams as ViewGroup.MarginLayoutParams).apply {

                            val metrics = DisplayMetrics()

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val display = requireActivity().display
                                display?.getRealMetrics(metrics)
                            } else {
                                @Suppress("DEPRECATION")
                                val display = requireActivity().windowManager.defaultDisplay
                                @Suppress("DEPRECATION")
                                display.getMetrics(metrics)
                            }

                            val dimensions: Pair<Int, Int> =
                                if (wallpaper.intrinsicWidth >= wallpaper.intrinsicHeight) {
                                    metrics.heightPixels to (wallpaper.intrinsicWidth) * metrics.heightPixels / (wallpaper.intrinsicHeight)
                                } else {
                                    metrics.widthPixels to (wallpaper.intrinsicHeight) * metrics.widthPixels / (wallpaper.intrinsicWidth)
                                }

                            setMargins(0, (-80).toPixel(requireContext()), 0, 0
                            )

                            width = dimensions.first
                            height = dimensions.second
                        }
                }
            } else {
                binding.widgetBg.setImageDrawable(null)
            }
        }

        viewModel.fragmentScrollY.observe(viewLifecycleOwner) {
            binding.toolbar.cardElevation = if (it > 0) 24f else 0f
        }

        viewModel.widgetAlign.observe(viewLifecycleOwner) {
            updatePreviewVisibility()
            lifecycleScope.launch {
                delay(350)
                updateClock()
            }
        }

        viewModel.showPreview.observe(viewLifecycleOwner) {
            updatePreviewVisibility()
        }

        viewModel.clockPreferencesUpdate.observe(viewLifecycleOwner) {
            updateClock()
        }

        viewModel.widgetPreferencesUpdate.observe(viewLifecycleOwner) {
            onUpdateUiEvent()
        }

        viewModel.showClock.observe(viewLifecycleOwner) {
            updateClockVisibility(it)
        }
    }

    private var uiJob: Job? = null

    private fun updateUI() {
        if (Preferences.showPreview) {
            lifecycleScope.launch(Dispatchers.IO) {
                val bgColor: Int = ContextCompat.getColor(
                    requireContext(),
                    if (ColorHelper.getFontColor(requireActivity().isDarkTheme())
                            .isColorDark()
                    ) android.R.color.white else R.color.colorAccent
                )

                val wallpaperDrawable = BitmapHelper.getTintedDrawable(
                    requireContext(),
                    R.drawable.card_background,
                    ColorHelper.getBackgroundColor(requireActivity().isDarkTheme())
                )

                withContext(Dispatchers.Main) {
                    binding.preview.setCardBackgroundColor(bgColor)
                    binding.widgetDetail.widgetShapeBackground.setImageDrawable(wallpaperDrawable)
                }
            }

            WidgetHelper.runWithCustomTypeface(requireContext()) { typeface ->
                uiJob?.cancel()
                uiJob = lifecycleScope.launch(Dispatchers.IO) {
                    val generatedView = MainWidget.getWidgetView(requireContext(), typeface)?.root

                    if (generatedView != null) {
                        withContext(Dispatchers.Main) {

                            binding.widgetDetail.content.removeAllViews()
                            val container = LinearLayout(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                            }
                            container.gravity = when (Preferences.widgetAlign) {
                                Constants.WidgetAlign.CENTER.rawValue -> Gravity.CENTER_HORIZONTAL
                                Constants.WidgetAlign.LEFT.rawValue -> Gravity.START
                                Constants.WidgetAlign.RIGHT.rawValue -> Gravity.END
                                else -> Gravity.NO_GRAVITY
                            }
                            container.addView(generatedView)
                            binding.widgetDetail.content.addView(container)

                            binding.widgetLoader.animate().scaleX(0f).scaleY(0f).alpha(0f)
                                .setDuration(200L).start()
                            binding.widget.animate().alpha(1f).start()
                        }
                    }
                }
            }
        }
    }

    private fun updateClock() {
        // Clock
        binding.widgetDetail.time.setTextColor(ColorHelper.getClockFontColor(requireActivity().isDarkTheme()))
        binding.widgetDetail.timeAmPm.setTextColor(ColorHelper.getClockFontColor(requireActivity().isDarkTheme()))
        binding.widgetDetail.time.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            Preferences.clockTextSize.toPixel(requireContext())
        )
        binding.widgetDetail.timeAmPm.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            Preferences.clockTextSize.toPixel(requireContext()) / 5 * 2
        )
        binding.widgetDetail.timeAmPm.isVisible = Preferences.showAMPMIndicator

        // Timezones
        if (Preferences.altTimezoneId != "" && Preferences.altTimezoneLabel != "") {
            // Clock
            binding.widgetDetail.altTimezoneTime.timeZone = Preferences.altTimezoneId
            binding.widgetDetail.altTimezoneTimeAmPm.timeZone = Preferences.altTimezoneId
            binding.widgetDetail.altTimezoneLabel.text = Preferences.altTimezoneLabel
            binding.widgetDetail.altTimezoneTime.setTextColor(ColorHelper.getClockFontColor(requireActivity().isDarkTheme()))
            binding.widgetDetail.altTimezoneTimeAmPm.setTextColor(ColorHelper.getClockFontColor(requireActivity().isDarkTheme()))
            binding.widgetDetail.altTimezoneLabel.setTextColor(ColorHelper.getClockFontColor(requireActivity().isDarkTheme()))
            binding.widgetDetail.altTimezoneTime.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                Preferences.clockTextSize.toPixel(requireContext()) / 3
            )
            binding.widgetDetail.altTimezoneTimeAmPm.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                (Preferences.clockTextSize.toPixel(requireContext()) / 3) / 5 * 2
            )
            binding.widgetDetail.altTimezoneLabel.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                (Preferences.clockTextSize.toPixel(requireContext()) / 3) / 5 * 2
            )
            binding.widgetDetail.timezonesContainer.isVisible = true
        } else {
            binding.widgetDetail.timezonesContainer.isVisible = false
        }

        // Clock bottom margin
        binding.widgetDetail.clockBottomMarginNone.isVisible =
            Preferences.showClock && Preferences.clockBottomMargin == Constants.ClockBottomMargin.NONE.rawValue
        binding.widgetDetail.clockBottomMarginSmall.isVisible =
            Preferences.showClock && Preferences.clockBottomMargin == Constants.ClockBottomMargin.SMALL.rawValue
        binding.widgetDetail.clockBottomMarginMedium.isVisible =
            Preferences.showClock && Preferences.clockBottomMargin == Constants.ClockBottomMargin.MEDIUM.rawValue
        binding.widgetDetail.clockBottomMarginLarge.isVisible =
            Preferences.showClock && Preferences.clockBottomMargin == Constants.ClockBottomMargin.LARGE.rawValue

        // Align
        binding.widgetDetail.timeContainer.layoutParams = (binding.widgetDetail.timeContainer.layoutParams as LinearLayout.LayoutParams).apply {
            gravity = when (Preferences.widgetAlign) {
                Constants.WidgetAlign.CENTER.rawValue -> Gravity.CENTER_HORIZONTAL
                Constants.WidgetAlign.LEFT.rawValue -> Gravity.START
                Constants.WidgetAlign.RIGHT.rawValue -> Gravity.END
                else -> Gravity.NO_GRAVITY
            }
        }
        if (Preferences.widgetAlign == Constants.WidgetAlign.RIGHT.rawValue) {
            with (binding.widgetDetail.timeContainer) {
                val child = getChildAt(2)
                if (child.id == R.id.timezones_container) {
                    removeViewAt(2)
                    child.layoutParams = (child.layoutParams as ViewGroup.MarginLayoutParams).apply {
                        marginEnd = 16f.convertDpToPixel(requireContext()).toInt()
                    }
                    addView(child, 0)
                }
            }
        } else {
            with (binding.widgetDetail.timeContainer) {
                val child = getChildAt(0)
                if (child.id == R.id.timezones_container) {
                    removeViewAt(0)
                    child.layoutParams = (child.layoutParams as ViewGroup.MarginLayoutParams).apply {
                        marginEnd = 0
                    }
                    addView(child, 2)
                }
            }
        }
    }

    private fun updateClockVisibility(showClock: Boolean) {
        binding.widgetDetail.timeContainer.clearAnimation()
        binding.widgetDetail.time.clearAnimation()

        updatePreviewVisibility()

        if (showClock) {
            binding.widgetDetail.timeContainer.layoutParams = (binding.widgetDetail.timeContainer.layoutParams as LinearLayout.LayoutParams).apply {
                height = RelativeLayout.LayoutParams.WRAP_CONTENT
            }
            binding.widgetDetail.timeContainer.measure(0, 0)
        }

        if ((Preferences.showClock && binding.widgetDetail.time.alpha != 1f) || (!Preferences.showClock && binding.widgetDetail.time.alpha != 0f)) {
            val initialHeight = binding.widgetDetail.timeContainer.measuredHeight
            ValueAnimator.ofFloat(
                if (showClock) 0f else 1f,
                if (showClock) 1f else 0f
            ).apply {
                duration = 500L
                addUpdateListener {
                    val animatedValue = animatedValue as Float
                    binding.widgetDetail.timeContainer.layoutParams =
                        binding.widgetDetail.timeContainer.layoutParams.apply {
                            height = (initialHeight * animatedValue).toInt()
                        }
                    binding.widgetDetail.time.alpha = animatedValue
                    binding.widgetDetail.timeAmPm.alpha = animatedValue
                    binding.widgetDetail.altTimezoneTime.alpha = animatedValue
                    binding.widgetDetail.altTimezoneTimeAmPm.alpha = animatedValue
                    binding.widgetDetail.altTimezoneLabel.alpha = animatedValue
                }
            }.start()
        }
    }

    private fun updatePreviewVisibility() {
        binding.preview.clearAnimation()
        if (binding.preview.layoutParams.height != (if (Preferences.showPreview) PREVIEW_BASE_HEIGHT.toPixel(requireContext()) else 0) + (if (Preferences.showClock) 100.toPixel(
                requireContext()
            ) else 0)) {
            ValueAnimator.ofInt(
                binding.preview.height,
                (if (Preferences.showPreview) PREVIEW_BASE_HEIGHT.toPixel(requireContext()) else 0) + (if (Preferences.showClock) 100.toPixel(
                    requireContext()
                ) else 0)
            ).apply {
                duration = 500L
                addUpdateListener {
                    val animatedValue = animatedValue as Int
                    val layoutParams = binding.preview.layoutParams
                    layoutParams.height = animatedValue
                    binding.preview.layoutParams = layoutParams
                }
            }.start()
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
        updateUI()
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    private var delayJob: Job? = null

    class UpdateUiMessageEvent
    class ChangeTabEvent

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateUiEvent() {
        delayJob?.cancel()
        delayJob = lifecycleScope.launch(Dispatchers.IO) {
            delay(300)
            withContext(Dispatchers.Main) {
                updateUI()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChangeTabEvent(ignore: ChangeTabEvent) {
        val navHost = childFragmentManager.findFragmentById(R.id.settings_fragment) as? NavHostFragment?
        navHost?.navController?.navigateUp()
    }
}
