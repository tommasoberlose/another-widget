package com.tommasoberlose.anotherwidget.ui.fragments.tabs

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chibatching.kotpref.blockingBulk
import com.google.android.material.transition.MaterialSharedAxis
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetColorPicker
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.databinding.FragmentGeneralSettingsBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.ColorHelper
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.toHexValue
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.toIntValue
import com.tommasoberlose.anotherwidget.helpers.DateHelper
import com.tommasoberlose.anotherwidget.ui.activities.tabs.CustomDateActivity
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.isDarkTheme
import kotlinx.android.synthetic.main.fragment_calendar_settings.*
import kotlinx.android.synthetic.main.fragment_clock_settings.*
import kotlinx.android.synthetic.main.fragment_general_settings.*
import kotlinx.android.synthetic.main.fragment_tab_selector.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class LayoutFragment : Fragment() {

    companion object {
        fun newInstance() = LayoutFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var colors: IntArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        val binding = DataBindingUtil.inflate<FragmentGeneralSettingsBinding>(inflater, R.layout.fragment_tab_layout, container, false)

        subscribeUi(viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.isDarkModeEnabled = activity?.isDarkTheme() == true

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        show_dividers_toggle.isChecked = Preferences.showDividers

        setupListener()
        lifecycleScope.launch(Dispatchers.IO) {
            val lazyColors = requireContext().resources.getIntArray(R.array.material_colors)
            withContext(Dispatchers.Main) {
                colors = lazyColors
            }
        }

        scrollView?.viewTreeObserver?.addOnScrollChangedListener {
            viewModel.fragmentScrollY.value = scrollView?.scrollY ?: 0
        }
    }


    @SuppressLint("DefaultLocale")
    private fun subscribeUi(
        viewModel: MainViewModel
    ) {

        viewModel.secondRowTopMargin.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                second_row_top_margin_label?.text = when (it) {
                    Constants.SecondRowTopMargin.NONE.value -> getString(R.string.settings_clock_bottom_margin_subtitle_none)
                    Constants.SecondRowTopMargin.SMALL.value -> getString(R.string.settings_clock_bottom_margin_subtitle_small)
                    Constants.SecondRowTopMargin.LARGE.value -> getString(R.string.settings_clock_bottom_margin_subtitle_large)
                    else -> getString(R.string.settings_clock_bottom_margin_subtitle_medium)
                }
            }
        })

        viewModel.backgroundCardColor.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (Preferences.backgroundCardAlpha == "00") {
                    background_color_label?.text = getString(R.string.transparent)
                } else {
                    background_color_label?.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getBackgroundColor(activity?.isDarkTheme() == true))).toUpperCase()
                }
            }
        })

        viewModel.backgroundCardColorDark.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (Preferences.backgroundCardAlphaDark == "00") {
                    background_color_label?.text = getString(R.string.transparent)
                } else {
                    background_color_label?.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getBackgroundColor(activity?.isDarkTheme() == true))).toUpperCase()
                }
            }
        })

        viewModel.backgroundCardAlpha.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (Preferences.backgroundCardAlpha == "00") {
                    background_color_label?.text = getString(R.string.transparent)
                } else {
                    background_color_label?.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getBackgroundColor(activity?.isDarkTheme() == true))).toUpperCase()
                }
            }
        })

        viewModel.backgroundCardAlphaDark.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (Preferences.backgroundCardAlphaDark == "00") {
                    background_color_label?.text = getString(R.string.transparent)
                } else {
                    background_color_label?.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getBackgroundColor(activity?.isDarkTheme() == true))).toUpperCase()
                }
            }
        })

        viewModel.dateFormat.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                date_format_label?.text = DateHelper.getDateText(requireContext(), Calendar.getInstance())
            }
        })

        viewModel.showDividers.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_dividers_label?.text =
                    if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
        })
    }

    private fun setupListener() {

        action_second_row_top_margin_size.setOnClickListener {
            BottomSheetMenu<Int>(
                requireContext(),
                header = getString(R.string.settings_secondary_row_top_margin_title)
            ).setSelectedValue(Preferences.secondRowTopMargin)
                .addItem(
                    getString(R.string.settings_clock_bottom_margin_subtitle_none),
                    Constants.SecondRowTopMargin.NONE.value
                )
                .addItem(
                    getString(R.string.settings_clock_bottom_margin_subtitle_small),
                    Constants.SecondRowTopMargin.SMALL.value
                )
                .addItem(
                    getString(R.string.settings_clock_bottom_margin_subtitle_medium),
                    Constants.SecondRowTopMargin.MEDIUM.value
                )
                .addItem(
                    getString(R.string.settings_clock_bottom_margin_subtitle_large),
                    Constants.SecondRowTopMargin.LARGE.value
                )
                .addOnSelectItemListener { value ->
                    Preferences.secondRowTopMargin = value
                }.show()
        }

        action_date_format.setOnClickListener {
            val now = Calendar.getInstance()
            val dialog = BottomSheetMenu<String>(requireContext(), header = getString(R.string.settings_date_format_title)).setSelectedValue(Preferences.dateFormat)

            dialog.addItem(DateHelper.getDefaultDateText(requireContext(), now), "")
            if (Preferences.dateFormat != "") {
                dialog.addItem(DateHelper.getDateText(requireContext(), now), Preferences.dateFormat)
            }
            dialog.addItem(getString(R.string.custom_date_format), "-")

            dialog.addOnSelectItemListener { value ->
                when (value) {
                    "-" -> {
                        startActivity(Intent(requireContext(), CustomDateActivity::class.java))
                    }
                    "" -> {
                        Preferences.blockingBulk {
                            isDateCapitalize = false
                            isDateUppercase = false
                        }
                        Preferences.dateFormat = value
                    }
                    else -> {
                        Preferences.dateFormat = value
                    }
                }
            }.show()
        }

        action_background_color.setOnClickListener {
            BottomSheetColorPicker(requireContext(),
                colors = colors,
                header = getString(R.string.settings_background_color_title),
                getSelected = { ColorHelper.getBackgroundColorRgb(activity?.isDarkTheme() == true) },
                onColorSelected = { color: Int ->
                    val colorString = Integer.toHexString(color)
                    if (activity?.isDarkTheme() == true) {
                        Preferences.backgroundCardColorDark =
                            "#" + if (colorString.length > 6) colorString.substring(2) else colorString
                    } else {
                        Preferences.backgroundCardColor =
                            "#" + if (colorString.length > 6) colorString.substring(2) else colorString
                    }
                },
                showAlphaSelector = true,
                alpha = if (activity?.isDarkTheme() == true) Preferences.backgroundCardAlphaDark.toIntValue() else Preferences.backgroundCardAlpha.toIntValue(),
                onAlphaChangeListener = { alpha ->
                    if (activity?.isDarkTheme() == true) {
                        Preferences.backgroundCardAlphaDark = alpha.toHexValue()
                    } else {
                        Preferences.backgroundCardAlpha = alpha.toHexValue()
                    }
                }
            ).show()
        }

        action_show_dividers.setOnClickListener {
            show_dividers_toggle.isChecked = !show_dividers_toggle.isChecked
        }

        show_dividers_toggle.setOnCheckedChangeListener { _, isChecked ->
            Preferences.showDividers = isChecked
        }
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
