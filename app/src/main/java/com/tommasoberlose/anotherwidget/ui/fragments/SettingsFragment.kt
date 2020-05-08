package com.tommasoberlose.anotherwidget.ui.fragments

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tommasoberlose.anotherwidget.BuildConfig
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.databinding.FragmentAdvancedSettingsBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.activities.SupportDevActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.openURI
import kotlinx.android.synthetic.main.fragment_advanced_settings.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SettingsFragment : Fragment() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis.create(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis.create(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        val binding = DataBindingUtil.inflate<FragmentAdvancedSettingsBinding>(inflater, R.layout.fragment_advanced_settings, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        action_back.setOnClickListener {
            Navigation.findNavController(it).popBackStack()
        }

        subscribeUi(viewModel)

        setupListener()

        app_version.text = "v%s (%s)".format(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }

    private fun subscribeUi(
        viewModel: MainViewModel
    ) {
        viewModel.darkThemePreference.observe(viewLifecycleOwner, Observer {
            AppCompatDelegate.setDefaultNightMode(it)
            maintainScrollPosition {
                theme?.text = when (it) {
                    AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.settings_subtitle_dark_theme_light)
                    AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.settings_subtitle_dark_theme_dark)
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY -> getString(R.string.settings_subtitle_dark_theme_by_battery_saver)
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> getString(R.string.settings_subtitle_dark_theme_follow_system)
                    else -> ""
                }
            }
        })

        viewModel.showPreview.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_widget_preview_label?.text =
                    if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
        })

        viewModel.showWallpaper.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_wallpaper_label?.text =
                    if (it && activity?.checkGrantedPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == true) getString(
                        R.string.settings_visible
                    ) else getString(R.string.settings_not_visible)
            }
        })
    }

    private fun setupListener() {
        action_change_theme.setOnClickListener {
            maintainScrollPosition {
                BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_theme_title))
                    .setSelectedValue(Preferences.darkThemePreference)
                    .addItem(
                        getString(R.string.settings_subtitle_dark_theme_light),
                        AppCompatDelegate.MODE_NIGHT_NO
                    )
                    .addItem(
                        getString(R.string.settings_subtitle_dark_theme_dark),
                        AppCompatDelegate.MODE_NIGHT_YES
                    )
                    .addItem(
                        getString(R.string.settings_subtitle_dark_theme_default),
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                    )
                    .addOnSelectItemListener { value ->
                        Preferences.darkThemePreference = value
                    }.show()
            }
        }

        action_show_widget_preview.setOnClickListener {
            maintainScrollPosition {
                BottomSheetMenu<Boolean>(requireContext(), header = getString(R.string.action_show_widget_preview))
                    .setSelectedValue(Preferences.showPreview)
                    .addItem(
                        getString(R.string.settings_visible),
                        true
                    )
                    .addItem(
                        getString(R.string.settings_not_visible),
                        false
                    )
                    .addOnSelectItemListener { value ->
                        Preferences.showPreview = value
                    }.show()
            }
        }

        action_show_wallpaper.setOnClickListener {
            maintainScrollPosition {
                BottomSheetMenu<Boolean>(requireContext(), header = getString(R.string.settings_title_show_wallpaper))
                    .setSelectedValue(Preferences.showWallpaper && activity?.checkGrantedPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == true)
                    .addItem(
                        getString(R.string.settings_visible),
                        true
                    )
                    .addItem(
                        getString(R.string.settings_not_visible),
                        false
                    )
                    .addOnSelectItemListener { value ->
                        if (value) {
                            requirePermission()
                        } else {
                            Preferences.showWallpaper = value
                        }
                    }.show()
            }
        }

        action_translate.setOnClickListener {
            activity?.openURI("https://github.com/tommasoberlose/another-widget/blob/master/app/src/main/res/values/strings.xml")
        }

        action_website.setOnClickListener {
            activity?.openURI("http://tommasoberlose.com/")
        }

        action_feedback.setOnClickListener {
            activity?.openURI("https://github.com/tommasoberlose/another-widget/issues")
        }

        action_help_dev.setOnClickListener {
            startActivity(Intent(requireContext(), SupportDevActivity::class.java))
        }

        action_refresh_widget.setOnClickListener {
            WeatherHelper.updateWeather(requireContext())
            CalendarHelper.updateEventList(requireContext())
            MediaPlayerHelper.updatePlayingMediaInfo(requireContext())
        }
    }

    private fun maintainScrollPosition(callback: () -> Unit) {
        val scrollPosition = scrollView.scrollY
        callback.invoke()
        lifecycleScope.launch {
            delay(200)
            scrollView.smoothScrollTo(0, scrollPosition)
        }
    }

    private fun requirePermission() {
        Dexter.withContext(requireContext())
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        Preferences.showWallpaper = false
                        Preferences.showWallpaper = report.areAllPermissionsGranted()
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    // Remember to invoke this method when the custom rationale is closed
                    // or just by default if you don't want to use any custom rationale.
                    token?.continuePermissionRequest()
                }
            })
            .check()
    }
}
