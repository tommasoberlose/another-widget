package com.tommasoberlose.anotherwidget.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chibatching.kotpref.blockingBulk
import com.chibatching.kotpref.bulk
import com.google.android.material.transition.MaterialSharedAxis
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetColorPicker
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.databinding.FragmentGeneralSettingsBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.helpers.ColorHelper
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.toHexValue
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.toIntValue
import com.tommasoberlose.anotherwidget.helpers.DateHelper
import com.tommasoberlose.anotherwidget.helpers.SettingsStringHelper
import com.tommasoberlose.anotherwidget.helpers.WidgetHelper
import com.tommasoberlose.anotherwidget.ui.activities.ChooseApplicationActivity
import com.tommasoberlose.anotherwidget.ui.activities.CustomDateActivity
import com.tommasoberlose.anotherwidget.ui.activities.CustomFontActivity
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.isDarkTheme
import kotlinx.android.synthetic.main.fragment_clock_settings.*
import kotlinx.android.synthetic.main.fragment_general_settings.*
import kotlinx.android.synthetic.main.fragment_general_settings.scrollView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class GeneralTabFragment : Fragment() {

    companion object {
        fun newInstance() = GeneralTabFragment()
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
        val binding = DataBindingUtil.inflate<FragmentGeneralSettingsBinding>(inflater, R.layout.fragment_general_settings, container, false)

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
    }


    @SuppressLint("DefaultLocale")
    private fun subscribeUi(
        viewModel: MainViewModel
    ) {

        viewModel.textMainSize.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                main_text_size_label?.text = String.format("%.0fsp", it)
            }
        })

        viewModel.textSecondSize.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                second_text_size_label?.text = String.format("%.0fsp", it)
            }
        })

        viewModel.textGlobalColor.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (Preferences.textGlobalAlpha == "00") {
                    font_color_label?.text = getString(R.string.transparent)
                } else {
                    font_color_label?.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getFontColor(activity?.isDarkTheme() == true))).toUpperCase()
                }
            }
        })

        viewModel.textGlobalColorDark.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (Preferences.textGlobalAlphaDark == "00") {
                    font_color_label?.text = getString(R.string.transparent)
                } else {
                    font_color_label?.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getFontColor(activity?.isDarkTheme() == true))).toUpperCase()
                }
            }
        })

        viewModel.textGlobalAlpha.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (Preferences.textGlobalAlpha == "00") {
                    font_color_label?.text = getString(R.string.transparent)
                } else {
                    font_color_label?.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getFontColor(activity?.isDarkTheme() == true))).toUpperCase()
                }
            }
        })

        viewModel.textGlobalAlphaDark.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (Preferences.textGlobalAlphaDark == "00") {
                    font_color_label?.text = getString(R.string.transparent)
                } else {
                    font_color_label?.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getFontColor(activity?.isDarkTheme() == true))).toUpperCase()
                }
            }
        })

        viewModel.textSecondaryColor.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (Preferences.textSecondaryAlpha == "00") {
                    secondary_font_color_label?.text = getString(R.string.transparent)
                } else {
                    secondary_font_color_label?.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getSecondaryFontColor(activity?.isDarkTheme() == true))).toUpperCase()
                }
            }
        })

        viewModel.textSecondaryColorDark.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (Preferences.textSecondaryAlphaDark == "00") {
                    secondary_font_color_label?.text = getString(R.string.transparent)
                } else {
                    secondary_font_color_label?.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getSecondaryFontColor(activity?.isDarkTheme() == true))).toUpperCase()
                }
            }
        })

        viewModel.textSecondaryAlpha.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (Preferences.textSecondaryAlpha == "00") {
                    secondary_font_color_label?.text = getString(R.string.transparent)
                } else {
                    secondary_font_color_label?.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getSecondaryFontColor(activity?.isDarkTheme() == true))).toUpperCase()
                }
            }
        })

        viewModel.textSecondaryAlphaDark.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (Preferences.textSecondaryAlphaDark == "00") {
                    secondary_font_color_label?.text = getString(R.string.transparent)
                } else {
                    secondary_font_color_label?.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getSecondaryFontColor(activity?.isDarkTheme() == true))).toUpperCase()
                }
            }
        })

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

        viewModel.textShadow.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (activity?.isDarkTheme() != true) {
                    text_shadow_label?.text =
                        getString(SettingsStringHelper.getTextShadowString(it))
                }
            }
        })

        viewModel.textShadowDark.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                if (activity?.isDarkTheme() == true) {
                    text_shadow_label?.text =
                        getString(SettingsStringHelper.getTextShadowString(it))
                }
            }
        })

        viewModel.dateFormat.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                date_format_label?.text = DateHelper.getDateText(requireContext(), Calendar.getInstance())
            }
        })

        viewModel.customFont.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                custom_font_label?.text = SettingsStringHelper.getCustomFontLabel(requireContext(), it)
                MainWidget.updateWidget(requireContext())
            }
        })

        viewModel.customFontFile.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                custom_font_label?.text = SettingsStringHelper.getCustomFontLabel(requireContext(), Preferences.customFont)
                MainWidget.updateWidget(requireContext())
            }
        })

        viewModel.customFontName.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                custom_font_label?.text = SettingsStringHelper.getCustomFontLabel(requireContext(), Preferences.customFont)
                MainWidget.updateWidget(requireContext())
            }
        })

        viewModel.customFontVariant.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                custom_font_label?.text = SettingsStringHelper.getCustomFontLabel(requireContext(), Preferences.customFont)
                MainWidget.updateWidget(requireContext())
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
        action_main_text_size.setOnClickListener {
            val dialog = BottomSheetMenu<Float>(requireContext(), header = getString(R.string.title_main_text_size)).setSelectedValue(Preferences.textMainSize)
            (40 downTo 10).filter { it % 2 == 0 }.forEach {
                dialog.addItem("${it}sp", it.toFloat())
            }
            dialog.addOnSelectItemListener { value ->
                    Preferences.textMainSize = value
            }.show()
        }

        action_second_text_size.setOnClickListener {
            val dialog = BottomSheetMenu<Float>(requireContext(), header = getString(R.string.title_second_text_size)).setSelectedValue(Preferences.textSecondSize)
            (40 downTo 10).filter { it % 2 == 0 }.forEach {
                dialog.addItem("${it}sp", it.toFloat())
            }
            dialog.addOnSelectItemListener { value ->
                Preferences.textSecondSize = value
            }.show()
        }

        action_font_color.setOnClickListener {
            BottomSheetColorPicker(requireContext(),
                colors = colors,
                header = getString(R.string.settings_font_color_title),
                getSelected = { ColorHelper.getFontColorRgb(activity?.isDarkTheme() == true) },
                onColorSelected = { color: Int ->
                    val colorString = Integer.toHexString(color)
                    if (activity?.isDarkTheme() == true) {
                        Preferences.textGlobalColorDark = "#" + if (colorString.length > 6) colorString.substring(2) else colorString
                    } else {
                        Preferences.textGlobalColor = "#" + if (colorString.length > 6) colorString.substring(2) else colorString
                    }
                },
                showAlphaSelector = true,
                alpha = if (activity?.isDarkTheme() == true) Preferences.textGlobalAlphaDark.toIntValue() else Preferences.textGlobalAlpha.toIntValue(),
                onAlphaChangeListener = { alpha ->
                    if (activity?.isDarkTheme() == true) {
                        Preferences.textGlobalAlphaDark = alpha.toHexValue()
                    } else {
                        Preferences.textGlobalAlpha = alpha.toHexValue()
                    }
                }
            ).show()
        }

        action_secondary_font_color.setOnClickListener {
            BottomSheetColorPicker(requireContext(),
                colors = colors,
                header = getString(R.string.settings_secondary_font_color_title),
                getSelected = { ColorHelper.getSecondaryFontColorRgb(activity?.isDarkTheme() == true) },
                onColorSelected = { color: Int ->
                    val colorString = Integer.toHexString(color)
                    if (activity?.isDarkTheme() == true) {
                        Preferences.textSecondaryColorDark =
                            "#" + if (colorString.length > 6) colorString.substring(2) else colorString
                    } else {
                        Preferences.textSecondaryColor =
                            "#" + if (colorString.length > 6) colorString.substring(2) else colorString
                    }
                },
                showAlphaSelector = true,
                alpha = if (activity?.isDarkTheme() == true) Preferences.textSecondaryAlphaDark.toIntValue() else Preferences.textSecondaryAlpha.toIntValue(),
                onAlphaChangeListener = { alpha ->
                    if (activity?.isDarkTheme() == true) {
                        Preferences.textSecondaryAlphaDark = alpha.toHexValue()
                    } else {
                        Preferences.textSecondaryAlpha = alpha.toHexValue()
                    }
                }
            ).show()
        }

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

        action_text_shadow.setOnClickListener {
            val dialog = BottomSheetMenu<Int>(requireContext(), header = getString(R.string.title_text_shadow)).setSelectedValue(if (activity?.isDarkTheme() == true) Preferences.textShadowDark else Preferences.textShadow)
            (2 downTo 0).forEach {
                dialog.addItem(getString(SettingsStringHelper.getTextShadowString(it)), it)
            }
            dialog.addOnSelectItemListener { value ->
                if (activity?.isDarkTheme() == true) {
                    Preferences.textShadowDark = value
                } else {
                    Preferences.textShadow = value
                }
            }.show()
        }

        action_custom_font.setOnClickListener {
            val dialog = BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_custom_font_title)).setSelectedValue(Preferences.customFont)
            dialog.addItem(SettingsStringHelper.getCustomFontLabel(requireContext(), 0), 0)

            if (Preferences.customFont == Constants.CUSTOM_FONT_GOOGLE_SANS) {
                dialog.addItem(SettingsStringHelper.getCustomFontLabel(requireContext(), Constants.CUSTOM_FONT_GOOGLE_SANS), Constants.CUSTOM_FONT_GOOGLE_SANS)
            }

            if (Preferences.customFontFile != "") {
                dialog.addItem(SettingsStringHelper.getCustomFontLabel(requireContext(), Preferences.customFont), Constants.CUSTOM_FONT_DOWNLOADED)
            }
            dialog.addItem(getString(R.string.action_custom_font_to_search), Constants.CUSTOM_FONT_DOWNLOAD_NEW)
            dialog.addOnSelectItemListener { value ->
                if (value == Constants.CUSTOM_FONT_DOWNLOAD_NEW) {
                    startActivityForResult(
                        Intent(requireContext(), CustomFontActivity::class.java),
                        RequestCode.CUSTOM_FONT_CHOOSER_REQUEST_CODE.code
                    )
                } else if (value != Constants.CUSTOM_FONT_DOWNLOADED) {
                    Preferences.bulk {
                        customFont = value
                        customFontFile = ""
                        customFontName = ""
                        customFontVariant = ""
                    }
                }
            }.show()
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
