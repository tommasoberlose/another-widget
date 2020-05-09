package com.tommasoberlose.anotherwidget.ui.fragments

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
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.databinding.FragmentGlanceSettingsBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.AlarmHelper
import com.tommasoberlose.anotherwidget.helpers.GlanceProviderHelper
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.models.GlanceProvider
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.fragment_glance_settings.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.idik.lib.slimadapter.SlimAdapter
import java.util.*


class GlanceTabFragment : Fragment() {

    companion object {
        fun newInstance() = GlanceTabFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: SlimAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        val binding = DataBindingUtil.inflate<FragmentGlanceSettingsBinding>(inflater, R.layout.fragment_glance_settings, container, false)

        subscribeUi(binding, viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        list.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(requireContext())
        list.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<String>(R.layout.glance_provider_item) { item, injector ->
                injector
                    .text(R.id.title, item)
            }
            .register<GlanceProvider>(R.layout.glance_provider_item) { item, injector ->
                injector
                    .text(R.id.title, item.title)
                    .with<ImageView>(R.id.icon) {
                        it.setImageDrawable(ContextCompat.getDrawable(requireContext(), item.icon))
                    }
                    .with<TextView>(R.id.label) {
                        it.isVisible = item.label != ""
                        it.text = item.label
                    }
            }
            .attachTo(list)

        adapter.updateData(
            GlanceProviderHelper.getGlanceProviders()
                .mapNotNull { GlanceProviderHelper.getGlanceProviderById(requireContext(), it) }
        )

        val mIth = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: ViewHolder, target: ViewHolder
                ): Boolean {
                    val fromPos = viewHolder.adapterPosition
                    val toPos = target.adapterPosition
                    // move item in `fromPos` to `toPos` in adapter.
                    adapter.notifyItemMoved(fromPos, toPos)

                    val list = GlanceProviderHelper.getGlanceProviders()
                    Collections.swap(list, fromPos, toPos)
                    GlanceProviderHelper.saveGlanceProviderOrder(list)
                    return true
                }

                override fun onSwiped(
                    viewHolder: ViewHolder,
                    direction: Int
                ) {
                    // remove from adapter
                }
            })

        mIth.attachToRecyclerView(list)


        setupListener()
        updateNextAlarmWarningUi()
    }

    private fun subscribeUi(
        binding: FragmentGlanceSettingsBinding,
        viewModel: MainViewModel
    ) {

        viewModel.showGlance.observe(viewLifecycleOwner, Observer {
            binding.isGlanceVisible = it
        })

        viewModel.showMusic.observe(viewLifecycleOwner, Observer {
            checkNotificationPermission()
        })

        viewModel.showNextAlarm.observe(viewLifecycleOwner, Observer {
            updateNextAlarmWarningUi()
        })

    }

    private fun setupListener() {

        action_show_glance.setOnClickListener {
            Preferences.showGlance = !Preferences.showGlance
        }

        show_glance_switch.setOnCheckedChangeListener { _, enabled: Boolean ->
            Preferences.showGlance = enabled
        }

        action_show_music.setOnClickListener {
            if (Preferences.showGlance) {
                BottomSheetMenu<Boolean>(
                    requireContext(),
                    header = getString(R.string.settings_show_music_title)
                ).setSelectedValue(Preferences.showMusic)
                    .addItem(getString(R.string.settings_visible), true)
                    .addItem(getString(R.string.settings_not_visible), false)
                    .addOnSelectItemListener { value ->
                        Preferences.showMusic = value
                    }.show()
            }
        }

        action_show_next_alarm.setOnClickListener {
            if (Preferences.showGlance) {
                BottomSheetMenu<Boolean>(
                    requireContext(),
                    header = getString(R.string.settings_show_next_alarm_title)
                ).setSelectedValue(Preferences.showNextAlarm)
                    .addItem(getString(R.string.settings_visible), true)
                    .addItem(getString(R.string.settings_not_visible), false)
                    .addOnSelectItemListener { value ->
                        Preferences.showNextAlarm = value
                    }.show()
            }
        }
    }

    private fun updateNextAlarmWarningUi() {
        with(requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val alarm = nextAlarmClock
            if (AlarmHelper.isAlarmProbablyWrong(requireContext()) && alarm != null && alarm.showIntent != null) {
                val pm = requireContext().packageManager as PackageManager
                val appNameOrPackage = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(alarm.showIntent?.creatorPackage ?: "", 0))
                } catch (e: Exception) {
                    alarm.showIntent?.creatorPackage ?: ""
                }
                maintainScrollPosition {
                    show_next_alarm_warning.text =
                        getString(R.string.next_alarm_warning).format(appNameOrPackage)
                }
            } else {
                maintainScrollPosition {
                    show_next_alarm_label?.text = if (Preferences.showNextAlarm) getString(R.string.settings_visible) else getString(
                        R.string.settings_not_visible)
                }
            }
        }
    }

    private val nextAlarmChangeBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateNextAlarmWarningUi()
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.registerReceiver(nextAlarmChangeBroadcastReceiver, IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
    }

    override fun onStop() {
        activity?.unregisterReceiver(nextAlarmChangeBroadcastReceiver)
        super.onStop()
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
