package com.tommasoberlose.anotherwidget.ui.fragments.tabs

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.transition.MaterialSharedAxis
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetColorPicker
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.databinding.FragmentTabLayoutBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.ColorHelper
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.toHexValue
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.toIntValue
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.isDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LayoutFragment : Fragment() {

    companion object {
        fun newInstance() = LayoutFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var colors: IntArray
    private lateinit var binding: FragmentTabLayoutBinding

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
        binding = FragmentTabLayoutBinding.inflate(inflater)

        subscribeUi(viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.isDarkModeEnabled = requireActivity().isDarkTheme()

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.showDividersToggle.setCheckedImmediatelyNoEvent(Preferences.showDividers)

        setupListener()
        lifecycleScope.launch(Dispatchers.IO) {
            val lazyColors = requireContext().resources.getIntArray(R.array.material_colors)
            withContext(Dispatchers.Main) {
                colors = lazyColors
            }
        }

        binding.scrollView.viewTreeObserver?.addOnScrollChangedListener {
            viewModel.fragmentScrollY.value = binding.scrollView.scrollY
        }
    }


    @SuppressLint("DefaultLocale")
    private fun subscribeUi(
        viewModel: MainViewModel
    ) {

        viewModel.secondRowTopMargin.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.secondRowTopMarginLabel.text = when (it) {
                    Constants.SecondRowTopMargin.NONE.rawValue -> getString(R.string.settings_clock_bottom_margin_subtitle_none)
                    Constants.SecondRowTopMargin.SMALL.rawValue -> getString(R.string.settings_clock_bottom_margin_subtitle_small)
                    Constants.SecondRowTopMargin.LARGE.rawValue -> getString(R.string.settings_clock_bottom_margin_subtitle_large)
                    else -> getString(R.string.settings_clock_bottom_margin_subtitle_medium)
                }
            }
        }

        viewModel.widgetAlign.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.widgetAlignLabel.text = when (it) {
                    Constants.WidgetAlign.LEFT.rawValue -> getString(R.string.settings_widget_align_left_subtitle)
                    Constants.WidgetAlign.RIGHT.rawValue -> getString(R.string.settings_widget_align_right_subtitle)
                    Constants.WidgetAlign.CENTER.rawValue -> getString(R.string.settings_widget_align_center_subtitle)
                    else -> getString(R.string.settings_widget_align_center_subtitle)
                }
            }
        }

        viewModel.clockBottomMargin.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.clockBottomMarginLabel.text = when (it) {
                    Constants.ClockBottomMargin.NONE.rawValue -> getString(R.string.settings_clock_bottom_margin_subtitle_none)
                    Constants.ClockBottomMargin.SMALL.rawValue -> getString(R.string.settings_clock_bottom_margin_subtitle_small)
                    Constants.ClockBottomMargin.LARGE.rawValue -> getString(R.string.settings_clock_bottom_margin_subtitle_large)
                    else -> getString(R.string.settings_clock_bottom_margin_subtitle_medium)
                }
            }
        }

        viewModel.backgroundCardColor.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                if (ColorHelper.getBackgroundAlpha(requireActivity().isDarkTheme()) == 0) {
                    binding.backgroundColorLabel.text = getString(R.string.transparent)
                } else {
                    binding.backgroundColorLabel.text =
                        "#%s".format(Integer.toHexString(ColorHelper.getBackgroundColor(requireActivity().isDarkTheme()))).toUpperCase()
                }
            }
        }

        viewModel.showDividers.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.showDividersLabel.text =
                    if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
        }
    }

    private fun setupListener() {

        binding.actionSecondRowTopMarginSize.setOnClickListener {
            BottomSheetMenu<Int>(
                requireContext(),
                header = getString(R.string.settings_secondary_row_top_margin_title)
            ).setSelectedValue(Preferences.secondRowTopMargin)
                .addItem(
                    getString(R.string.settings_clock_bottom_margin_subtitle_none),
                    Constants.SecondRowTopMargin.NONE.rawValue
                )
                .addItem(
                    getString(R.string.settings_clock_bottom_margin_subtitle_small),
                    Constants.SecondRowTopMargin.SMALL.rawValue
                )
                .addItem(
                    getString(R.string.settings_clock_bottom_margin_subtitle_medium),
                    Constants.SecondRowTopMargin.MEDIUM.rawValue
                )
                .addItem(
                    getString(R.string.settings_clock_bottom_margin_subtitle_large),
                    Constants.SecondRowTopMargin.LARGE.rawValue
                )
                .addOnSelectItemListener { value ->
                    Preferences.secondRowTopMargin = value
                }.show()
        }

        binding.actionClockBottomMarginSize.setOnClickListener {
            BottomSheetMenu<Int>(
                requireContext(),
                header = getString(R.string.settings_clock_bottom_margin_title)
            ).setSelectedValue(Preferences.clockBottomMargin)
                .addItem(
                    getString(R.string.settings_clock_bottom_margin_subtitle_none),
                    Constants.ClockBottomMargin.NONE.rawValue
                )
                .addItem(
                    getString(R.string.settings_clock_bottom_margin_subtitle_small),
                    Constants.ClockBottomMargin.SMALL.rawValue
                )
                .addItem(
                    getString(R.string.settings_clock_bottom_margin_subtitle_medium),
                    Constants.ClockBottomMargin.MEDIUM.rawValue
                )
                .addItem(
                    getString(R.string.settings_clock_bottom_margin_subtitle_large),
                    Constants.ClockBottomMargin.LARGE.rawValue
                )
                .addOnSelectItemListener { value ->
                    Preferences.clockBottomMargin = value
                }.show()
        }

        binding.actionWidgetAlign.setOnClickListener {
            BottomSheetMenu<Int>(
                requireContext(),
                header = getString(R.string.settings_widget_align_title)
            ).setSelectedValue(Preferences.widgetAlign)
                .addItem(
                    getString(R.string.settings_widget_align_center_subtitle),
                    Constants.WidgetAlign.CENTER.rawValue
                )
                .addItem(
                    getString(R.string.settings_widget_align_left_subtitle),
                    Constants.WidgetAlign.LEFT.rawValue
                )
                .addItem(
                    getString(R.string.settings_widget_align_right_subtitle),
                    Constants.WidgetAlign.RIGHT.rawValue
                )
                .addOnSelectItemListener { value ->
                    Preferences.widgetAlign = value
                }.show()
        }

        binding.actionBackgroundColor.setOnClickListener {
            BottomSheetColorPicker(requireContext(),
                colors = colors,
                header = getString(R.string.settings_background_color_title),
                getSelected = { ColorHelper.getBackgroundColorRgb(requireActivity().isDarkTheme()) },
                onColorSelected = { color: Int ->
                    val colorString = Integer.toHexString(color)
                    if (requireActivity().isDarkTheme()) {
                        Preferences.backgroundCardColorDark =
                            "#" + if (colorString.length > 6) colorString.substring(2) else colorString
                    } else {
                        Preferences.backgroundCardColor =
                            "#" + if (colorString.length > 6) colorString.substring(2) else colorString
                    }
                },
                showAlphaSelector = true,
                alpha = if (requireActivity().isDarkTheme()) Preferences.backgroundCardAlphaDark.toIntValue() else Preferences.backgroundCardAlpha.toIntValue(),
                onAlphaChangeListener = { alpha ->
                    if (requireActivity().isDarkTheme()) {
                        Preferences.backgroundCardAlphaDark = alpha.toHexValue()
                    } else {
                        Preferences.backgroundCardAlpha = alpha.toHexValue()
                    }
                }
            ).show()
        }

        binding.actionShowDividers.setOnClickListener {
            binding.showDividersToggle.isChecked = !binding.showDividersToggle.isChecked
        }

        binding.showDividersToggle.setOnCheckedChangeListener { _, isChecked ->
            Preferences.showDividers = isChecked
        }
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
