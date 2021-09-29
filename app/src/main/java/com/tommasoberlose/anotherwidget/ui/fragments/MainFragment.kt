package com.tommasoberlose.anotherwidget.ui.fragments

import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
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

        viewModel.showPreview.observe(viewLifecycleOwner) {
            binding.preview.isVisible = it
        }

        viewModel.widgetPreferencesUpdate.observe(viewLifecycleOwner) {
            onUpdateUiEvent(null)
        }
    }

    private var uiJob: Job? = null

    private fun updateUI() {
        if (Preferences.showPreview) {
            WidgetHelper.runWithCustomTypeface(requireContext()) { typeface ->
                uiJob?.cancel()
                uiJob = lifecycleScope.launch(Dispatchers.IO) {
                    val generatedView = MainWidget.getWidgetView(requireContext(), binding.widget.width, typeface)

                    if (generatedView != null) {
                        val view: View = generatedView.apply(requireActivity().applicationContext, binding.widget)
                        view.measure(0, 0)

                        withContext(Dispatchers.Main) {
                            binding.widgetLoader.animate().alpha(0f).setDuration(200L).start()
                            binding.widget.animate().alpha(0f).setDuration(200L).withEndAction {
                                updatePreviewVisibility(view.measuredHeight)
                                binding.widget.removeAllViews()
                                binding.widget.addView(view)

                                binding.widget.animate().setStartDelay(300L).alpha(1f).start()
                            }.start()
                        }
                    }
                }
            }
        }
    }

    private fun updatePreviewVisibility(widgetHeight: Int) {
        if (isAdded) {
            val newHeight = widgetHeight + 32f.convertDpToPixel(requireContext()).toInt()
            if (binding.preview.layoutParams.height != newHeight) {
                binding.preview.clearAnimation()
                ValueAnimator.ofInt(
                    binding.preview.height,
                    newHeight
                ).apply {
                    duration = 300L
                    addUpdateListener {
                        val animatedValue = animatedValue as Int
                        val layoutParams = binding.preview.layoutParams
                        layoutParams.height = animatedValue
                        binding.preview.layoutParams = layoutParams
                    }
                }.start()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
//        updateUI()
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    private var delayJob: Job? = null

    class UpdateUiMessageEvent
    class ChangeTabEvent(val page: Int)

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateUiEvent(ignore: UpdateUiMessageEvent?) {
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
