package com.tommasoberlose.anotherwidget.ui.fragments

import android.app.Activity
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetColorPicker
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.components.FixedFocusScrollView
import com.tommasoberlose.anotherwidget.databinding.FragmentClockSettingsBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.helpers.AlarmHelper
import com.tommasoberlose.anotherwidget.helpers.ColorHelper
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.toHexValue
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.toIntValue
import com.tommasoberlose.anotherwidget.ui.activities.ChooseApplicationActivity
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.fragment_clock_settings.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception


class ClockTabFragment : Fragment() {

    companion object {
        fun newInstance() = ClockTabFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var colors: IntArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        val binding = DataBindingUtil.inflate<FragmentClockSettingsBinding>(inflater, R.layout.fragment_clock_settings, container, false)

        subscribeUi(binding, viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        ampm_indicator_toggle.isChecked = Preferences.showAMPMIndicator

        lifecycleScope.launch(Dispatchers.IO) {
            val lazyColors = requireContext().resources.getIntArray(R.array.material_colors)
            withContext(Dispatchers.Main) {
                colors = lazyColors
            }
        }
        setupListener()
    }

    private fun subscribeUi(
        binding: FragmentClockSettingsBinding,
        viewModel: MainViewModel
    ) {
        binding.isClockVisible = Preferences.showClock

        viewModel.showBigClockWarning.observe(viewLifecycleOwner, Observer {
            large_clock_warning?.isVisible = it
            small_clock_warning?.isVisible = !it
        })

        viewModel.showClock.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_clock_label?.text =
                    if (it) getString(R.string.show_clock_visible) else getString(R.string.show_clock_not_visible)
                binding.isClockVisible = it
            }
        })

        viewModel.clockTextSize.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                clock_text_size_label?.text = String.format("%.0fsp", it)
            }
        })

        viewModel.showAMPMIndicator.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                ampm_indicator_label?.text = if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
        })

        viewModel.clockTextColor.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (Preferences.clockTextAlpha == "00") {
                    clock_text_color_label?.text = getString(R.string.transparent)
                } else {
                    clock_text_color_label?.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getClockFontColor())).toUpperCase()
                }
            }
        })

        viewModel.clockTextAlpha.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (Preferences.clockTextAlpha == "00") {
                    clock_text_color_label?.text = getString(R.string.transparent)
                } else {
                    clock_text_color_label?.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getClockFontColor())).toUpperCase()
                }
            }
        })

        viewModel.clockBottomMargin.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                clock_bottom_margin_label?.text = when (it) {
                    Constants.ClockBottomMargin.NONE.value -> getString(R.string.settings_clock_bottom_margin_subtitle_none)
                    Constants.ClockBottomMargin.SMALL.value -> getString(R.string.settings_clock_bottom_margin_subtitle_small)
                    Constants.ClockBottomMargin.LARGE.value -> getString(R.string.settings_clock_bottom_margin_subtitle_large)
                    else -> getString(R.string.settings_clock_bottom_margin_subtitle_medium)
                }
            }
        })

        viewModel.clockAppName.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                clock_app_label?.text =
                    if (Preferences.clockAppName != "") Preferences.clockAppName else getString(R.string.default_clock_app)
            }
        })
    }

    private fun setupListener() {
        action_hide_large_clock_warning.setOnClickListener {
            Preferences.showBigClockWarning = false
        }

        action_show_clock.setOnClickListener {
            Preferences.showClock = !Preferences.showClock
        }

        show_clock_switch.setOnCheckedChangeListener { _, enabled: Boolean ->
            Preferences.showClock = enabled
        }

        action_clock_text_size.setOnClickListener {
            if (Preferences.showClock) {
                val dialog = BottomSheetMenu<Float>(
                    requireContext(),
                    header = getString(R.string.settings_clock_text_size_title)
                ).setSelectedValue(Preferences.clockTextSize)
                (46 downTo 12).filter { it % 2 == 0 }.forEach {
                    dialog.addItem("${it}sp", it.toFloat())
                }
                dialog.addOnSelectItemListener { value ->
                    Preferences.clockTextSize = value
                }.show()
            }
        }

        action_ampm_indicator_size.setOnClickListener {
            if (Preferences.showClock) {
                ampm_indicator_toggle.isChecked = !ampm_indicator_toggle.isChecked
            }
        }

        ampm_indicator_toggle.setOnCheckedChangeListener { _, isChecked ->
            if (Preferences.showClock) {
                Preferences.showAMPMIndicator = isChecked
            }
        }

        action_clock_text_color.setOnClickListener {
            if (Preferences.showClock) {
                BottomSheetColorPicker(requireContext(),
                    colors = colors,
                    header = getString(R.string.settings_font_color_title),
                    getSelected = ColorHelper::getClockFontColorRgb,
                    onColorSelected = { color: Int ->
                        val colorString = Integer.toHexString(color)
                        Preferences.clockTextColor =
                            "#" + if (colorString.length > 6) colorString.substring(2) else colorString
                    },
                    showAlphaSelector = true,
                    alpha = Preferences.clockTextAlpha.toIntValue(),
                    onAlphaChangeListener = { alpha ->
                        Preferences.clockTextAlpha = alpha.toHexValue()
                    }
                ).show()
            }
        }

        action_clock_bottom_margin_size.setOnClickListener {
            if (Preferences.showClock) {
                BottomSheetMenu<Int>(
                    requireContext(),
                    header = getString(R.string.settings_clock_bottom_margin_title)
                ).setSelectedValue(Preferences.clockBottomMargin)
                    .addItem(
                        getString(R.string.settings_clock_bottom_margin_subtitle_none),
                        Constants.ClockBottomMargin.NONE.value
                    )
                    .addItem(
                        getString(R.string.settings_clock_bottom_margin_subtitle_small),
                        Constants.ClockBottomMargin.SMALL.value
                    )
                    .addItem(
                        getString(R.string.settings_clock_bottom_margin_subtitle_medium),
                        Constants.ClockBottomMargin.MEDIUM.value
                    )
                    .addItem(
                        getString(R.string.settings_clock_bottom_margin_subtitle_large),
                        Constants.ClockBottomMargin.LARGE.value
                    )
                    .addOnSelectItemListener { value ->
                        Preferences.clockBottomMargin = value
                    }.show()
            }
        }

        action_clock_app.setOnClickListener {
            if (Preferences.showClock) {
                if (Preferences.showClock) {
                    startActivityForResult(
                        Intent(requireContext(), ChooseApplicationActivity::class.java),
                        RequestCode.CLOCK_APP_REQUEST_CODE.code
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode ==  RequestCode.CLOCK_APP_REQUEST_CODE.code) {
            Preferences.bulk {
                clockAppName = data?.getStringExtra(Constants.RESULT_APP_NAME) ?: getString(R.string.default_clock_app)
                clockAppPackage = data?.getStringExtra(Constants.RESULT_APP_PACKAGE) ?: ""
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun maintainScrollPosition(callback: () -> Unit) {
        scrollView.isScrollable = false
        callback.invoke()
        lifecycleScope.launch {
            delay(200)
            scrollView.isScrollable = true
        }
    }
}
