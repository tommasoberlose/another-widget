package com.tommasoberlose.anotherwidget.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chibatching.kotpref.bulk
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.databinding.FragmentMusicSettingsBinding
import com.tommasoberlose.anotherwidget.databinding.FragmentWeatherSettingsBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.helpers.SettingsStringHelper
import com.tommasoberlose.anotherwidget.receivers.WeatherReceiver
import com.tommasoberlose.anotherwidget.ui.activities.ChooseApplicationActivity
import com.tommasoberlose.anotherwidget.ui.activities.CustomLocationActivity
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.activities.WeatherProviderActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import kotlinx.android.synthetic.main.fragment_music_settings.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicTabFragment : Fragment() {

    companion object {
        fun newInstance() = MusicTabFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        val binding = DataBindingUtil.inflate<FragmentMusicSettingsBinding>(inflater, R.layout.fragment_music_settings, container, false)

        subscribeUi(binding, viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupListener()
    }

    private fun subscribeUi(
        binding: FragmentMusicSettingsBinding,
        viewModel: MainViewModel
    ) {

        viewModel.showMusic.observe(viewLifecycleOwner, Observer {
            binding.isMusicVisible = Preferences.showMusic
            checkNotificationPermission()
        })

        viewModel.mediaInfoFormat.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                label_music_info_format?.text =
                    if (it != "") it else getString(R.string.default_weather_app)
            }
        })
    }

    private fun setupListener() {
        action_show_music.setOnClickListener {
            Preferences.showMusic = !Preferences.showMusic
        }

        action_music_info_format.setOnClickListener {
            if (Preferences.showMusic) {
//                startActivityForResult(
//                    Intent(requireContext(), WeatherProviderActivity::class.java),
//                    RequestCode.WEATHER_PROVIDER_REQUEST_CODE.code
//                )
            }
        }

    }

    private fun checkNotificationPermission() {
        if (NotificationManagerCompat.getEnabledListenerPackages(requireContext()).contains(requireContext().packageName)) {
            notification_permission_alert?.isVisible = false
            MediaPlayerHelper.updatePlayingMediaInfo(requireContext())
            show_music_label?.text = if (Preferences.showMusic) getString(R.string.settings_show_music_enabled_subtitle) else getString(R.string.settings_show_music_disabled_subtitle)
        } else if (Preferences.showMusic) {
            notification_permission_alert?.isVisible = true
            show_music_label?.text = getString(R.string.settings_request_notification_access)
            notification_permission_alert?.setOnClickListener {
                activity?.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }
        } else {
            show_music_label?.text = getString(R.string.settings_show_music_disabled_subtitle)
            notification_permission_alert?.isVisible = false
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

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
    }
}
