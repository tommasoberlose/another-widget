package com.tommasoberlose.anotherwidget.ui.fragments.tabs

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chibatching.kotpref.blockingBulk
import com.chibatching.kotpref.bulk
import com.google.android.material.transition.MaterialSharedAxis
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetColorPicker
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.components.BottomSheetPicker
import com.tommasoberlose.anotherwidget.databinding.FragmentTabTypographyBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.helpers.ColorHelper
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.toHexValue
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.toIntValue
import com.tommasoberlose.anotherwidget.helpers.DateHelper
import com.tommasoberlose.anotherwidget.helpers.SettingsStringHelper
import com.tommasoberlose.anotherwidget.ui.activities.tabs.CustomFontActivity
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.activities.tabs.CustomDateActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.isDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class TypographyFragment : Fragment() {

    companion object {
        fun newInstance() = TypographyFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var colors: IntArray

    private lateinit var binding: FragmentTabTypographyBinding

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
        binding = FragmentTabTypographyBinding.inflate(inflater)

        subscribeUi(viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.isDarkModeEnabled = activity?.isDarkTheme() == true

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupListener()
        lifecycleScope.launch(Dispatchers.IO) {
            val lazyColors = requireContext().resources.getIntArray(R.array.material_colors)
            withContext(Dispatchers.Main) {
                colors = lazyColors
            }
        }

        binding.scrollView.viewTreeObserver.addOnScrollChangedListener {
            viewModel.fragmentScrollY.value = binding.scrollView.scrollY
        }
    }


    @SuppressLint("DefaultLocale")
    private fun subscribeUi(
        viewModel: MainViewModel
    ) {

        viewModel.textMainSize.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.mainTextSizeLabel.text = String.format("%.0fsp", it)
            }
        }

        viewModel.textSecondSize.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.secondTextSizeLabel.text = String.format("%.0fsp", it)
            }
        }

        viewModel.textGlobalColor.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                if (Preferences.textGlobalAlpha == "00") {
                    binding.fontColorLabel.text = getString(R.string.transparent)
                } else {
                    binding.fontColorLabel.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getFontColor(requireActivity().isDarkTheme()))).toUpperCase()
                }
            }
        }

        viewModel.textSecondaryColor.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                if (Preferences.textSecondaryAlpha == "00") {
                    binding.secondaryFontColorLabel.text = getString(R.string.transparent)
                } else {
                    binding.secondaryFontColorLabel.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getSecondaryFontColor(requireActivity().isDarkTheme()))).toUpperCase()
                }
            }
        }

        viewModel.textShadow.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                if (requireActivity().isDarkTheme()) {
                    binding.textShadowLabel.text =
                        getString(SettingsStringHelper.getTextShadowString(it))
                }
            }
        }

        viewModel.textShadow.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                if (!requireActivity().isDarkTheme()) {
                    binding.textShadowLabel.text =
                        getString(SettingsStringHelper.getTextShadowString(it))
                }
            }
        }

        viewModel.textShadowDark.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                if (requireActivity().isDarkTheme()) {
                    binding.textShadowLabel.text =
                        getString(SettingsStringHelper.getTextShadowString(it))
                }
            }
        }

        viewModel.font.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.customFontLabel.text = SettingsStringHelper.getCustomFontLabel(requireContext(), Preferences.customFont)
            }
        }

        viewModel.dateFormat.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.dateFormatLabel.text = DateHelper.getDateText(requireContext(), Calendar.getInstance())
            }
        }
    }

    private fun setupListener() {
        binding.actionMainTextSize.setOnClickListener {
            BottomSheetPicker(
                requireContext(),
                items = (40 downTo 10).map { BottomSheetPicker.MenuItem("${it}sp", it.toFloat()) },
                getSelected = { Preferences.textMainSize },
                header = getString(R.string.title_main_text_size),
                onItemSelected = {value ->
                    if (value != null) Preferences.textMainSize = value
                }
            ).show()
        }

        binding.actionSecondTextSize.setOnClickListener {
            BottomSheetPicker(
                requireContext(),
                items = (40 downTo 10).map { BottomSheetPicker.MenuItem("${it}sp", it.toFloat()) },
                getSelected = { Preferences.textSecondSize },
                header = getString(R.string.title_second_text_size),
                onItemSelected = {value ->
                    if (value != null) Preferences.textSecondSize = value
                }
            ).show()
        }

        binding.actionFontColor.setOnClickListener {
            BottomSheetColorPicker(requireContext(),
                colors = colors,
                header = getString(R.string.settings_font_color_title),
                getSelected = { ColorHelper.getFontColorRgb(requireActivity().isDarkTheme()) },
                onColorSelected = { color: Int ->
                    val colorString = Integer.toHexString(color)
                    if (requireActivity().isDarkTheme()) {
                        Preferences.textGlobalColorDark = "#" + if (colorString.length > 6) colorString.substring(2) else colorString
                    } else {
                        Preferences.textGlobalColor = "#" + if (colorString.length > 6) colorString.substring(2) else colorString
                    }
                },
                showAlphaSelector = true,
                alpha = if (requireActivity().isDarkTheme()) Preferences.textGlobalAlphaDark.toIntValue() else Preferences.textGlobalAlpha.toIntValue(),
                onAlphaChangeListener = { alpha ->
                    if (requireActivity().isDarkTheme()) {
                        Preferences.textGlobalAlphaDark = alpha.toHexValue()
                    } else {
                        Preferences.textGlobalAlpha = alpha.toHexValue()
                    }
                },
            ).show()
        }

        binding.actionSecondaryFontColor.setOnClickListener {
            BottomSheetColorPicker(requireContext(),
                colors = colors,
                header = getString(R.string.settings_secondary_font_color_title),
                getSelected = { ColorHelper.getSecondaryFontColorRgb(requireActivity().isDarkTheme()) },
                onColorSelected = { color: Int ->
                    val colorString = Integer.toHexString(color)
                    if (requireActivity().isDarkTheme()) {
                        Preferences.textSecondaryColorDark =
                            "#" + if (colorString.length > 6) colorString.substring(2) else colorString
                    } else {
                        Preferences.textSecondaryColor =
                            "#" + if (colorString.length > 6) colorString.substring(2) else colorString
                    }
                },
                showAlphaSelector = true,
                alpha = if (requireActivity().isDarkTheme()) Preferences.textSecondaryAlphaDark.toIntValue() else Preferences.textSecondaryAlpha.toIntValue(),
                onAlphaChangeListener = { alpha ->
                    if (requireActivity().isDarkTheme()) {
                        Preferences.textSecondaryAlphaDark = alpha.toHexValue()
                    } else {
                        Preferences.textSecondaryAlpha = alpha.toHexValue()
                    }
                },
            ).show()
        }

        binding.actionTextShadow.setOnClickListener {
            val dialog = BottomSheetMenu<Int>(requireContext(), header = getString(R.string.title_text_shadow)).setSelectedValue(if (requireActivity().isDarkTheme()) Preferences.textShadowDark else Preferences.textShadow)
            (2 downTo 0).forEach {
                dialog.addItem(getString(SettingsStringHelper.getTextShadowString(it)), it)
            }
            dialog.addOnSelectItemListener { value ->
                if (requireActivity().isDarkTheme()) {
                    Preferences.textShadowDark = value
                } else {
                    Preferences.textShadow = value
                }
            }.show()
        }

        binding.actionCustomFont.setOnClickListener {
            val dialog = BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_custom_font_title)).setSelectedValue(
                Preferences.customFont)
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
                } else if (value != Preferences.customFont) {
                    Preferences.bulk {
                        customFont = value
                        customFontFile = ""
                        customFontName = ""
                        customFontVariant = ""
                    }
                }
            }.show()
        }

        binding.actionDateFormat.setOnClickListener {
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == android.app.Activity.RESULT_OK) {
            when (requestCode) {
                RequestCode.CUSTOM_FONT_CHOOSER_REQUEST_CODE.code -> {
                    com.tommasoberlose.anotherwidget.ui.widgets.MainWidget.updateWidget(requireContext())
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun maintainScrollPosition(callback: () -> Unit) {
        binding.scrollView.isScrollable = false
        callback.invoke()
        lifecycleScope.launch {
            delay(200)
            binding.scrollView.isScrollable = true
        }
    }
}
