package com.tommasoberlose.anotherwidget.ui.fragments.tabs

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.transition.MaterialSharedAxis
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.CustomNotesDialog
import com.tommasoberlose.anotherwidget.components.GlanceSettingsDialog
import com.tommasoberlose.anotherwidget.databinding.FragmentTabGlanceBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.ActiveNotificationsHelper
import com.tommasoberlose.anotherwidget.helpers.AlarmHelper
import com.tommasoberlose.anotherwidget.helpers.GlanceProviderHelper
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.models.GlanceProvider
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.idik.lib.slimadapter.SlimAdapter


class GlanceTabFragment : Fragment() {

    companion object {
        fun newInstance() = GlanceTabFragment()
    }

    private var dialog: GlanceSettingsDialog? = null
    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: MainViewModel
    private val list: ArrayList<Constants.GlanceProviderId> by lazy {
        GlanceProviderHelper.getGlanceProviders(requireContext())
    }
    private lateinit var binding: FragmentTabGlanceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        binding = FragmentTabGlanceBinding.inflate(inflater)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // List
        binding.providersList.hasFixedSize()
        binding.providersList.isNestedScrollingEnabled = false
        val mLayoutManager = LinearLayoutManager(context)
        binding.providersList.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<GlanceProvider>(R.layout.glance_provider_item) { item, injector ->
                val provider = Constants.GlanceProviderId.from(item.id)!!
                injector
                    .text(R.id.title, item.title)
                    .with<ImageView>(R.id.icon) {
                        it.setImageDrawable(ContextCompat.getDrawable(requireContext(), item.icon))
                    }
                    .clicked(R.id.item) {
                        if (provider == Constants.GlanceProviderId.CUSTOM_INFO) {
                            CustomNotesDialog(requireContext()){
                                adapter.notifyItemRangeChanged(0, adapter.data.size)
                            }.show()
                        } else {
                            dialog = GlanceSettingsDialog(requireActivity(), provider) {
                                adapter.notifyItemRangeChanged(0, adapter.data.size)
                            }
                            dialog?.setOnDismissListener {
                                dialog = null
                            }
                            dialog?.show()
                        }
                    }
                var isVisible = false
                when (provider) {
                    Constants.GlanceProviderId.PLAYING_SONG -> {
                        when {
                            ActiveNotificationsHelper.checkNotificationAccess(requireContext()) -> {
                                MediaPlayerHelper.updatePlayingMediaInfo(requireContext())
                                injector.visibility(R.id.error_icon, View.GONE)
                                injector.visibility(R.id.info_icon, View.VISIBLE)
                                injector.text(
                                    R.id.label,
                                    if (Preferences.showMusic) getString(R.string.settings_visible) else getString(
                                        R.string.settings_not_visible
                                    )
                                )
                                isVisible = Preferences.showMusic
                            }
                            Preferences.showMusic -> {
                                injector.visibility(R.id.error_icon, View.VISIBLE)
                                injector.visibility(R.id.info_icon, View.GONE)
                                injector.text(R.id.label, getString(R.string.settings_not_visible))
                                isVisible = false
                            }
                            else -> {
                                injector.visibility(R.id.error_icon, View.GONE)
                                injector.visibility(R.id.info_icon, View.VISIBLE)
                                injector.text(R.id.label, getString(R.string.settings_not_visible))
                                isVisible = false
                            }
                        }
                    }
                    Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> {
                        injector.text(
                            R.id.label,
                            if (Preferences.showNextAlarm && !AlarmHelper.isAlarmProbablyWrong(
                                    requireContext()
                                )
                            ) getString(R.string.settings_visible) else getString(
                                R.string.settings_not_visible
                            )
                        )
                        injector.visibility(
                            R.id.error_icon,
                            if (Preferences.showNextAlarm && AlarmHelper.isAlarmProbablyWrong(
                                    requireContext()
                                )
                            ) View.VISIBLE else View.GONE
                        )
                        injector.visibility(
                            R.id.info_icon,
                            if (!(Preferences.showNextAlarm && AlarmHelper.isAlarmProbablyWrong(
                                    requireContext()
                                ))
                            ) View.VISIBLE else View.GONE
                        )
                        isVisible = (Preferences.showNextAlarm && !AlarmHelper.isAlarmProbablyWrong(
                            requireContext()
                        ))
                    }
                    Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> {
                        injector.text(
                            R.id.label,
                            if (Preferences.showBatteryCharging) getString(R.string.settings_visible) else getString(
                                R.string.settings_not_visible
                            )
                        )
                        injector.visibility(R.id.error_icon, View.GONE)
                        injector.visibility(R.id.info_icon, View.VISIBLE)
                        isVisible = Preferences.showBatteryCharging
                    }
                    Constants.GlanceProviderId.NOTIFICATIONS -> {
                        when {
                            ActiveNotificationsHelper.checkNotificationAccess(requireContext()) -> {
                                injector.visibility(R.id.error_icon, View.GONE)
                                injector.visibility(R.id.info_icon, View.VISIBLE)
                                injector.text(
                                    R.id.label,
                                    if (Preferences.showNotifications) getString(
                                        R.string.settings_visible
                                    ) else getString(R.string.settings_not_visible)
                                )
                                isVisible = Preferences.showNotifications
                            }
                            Preferences.showNotifications -> {
                                injector.visibility(R.id.error_icon, View.VISIBLE)
                                injector.visibility(R.id.info_icon, View.GONE)
                                injector.text(R.id.label, getString(R.string.settings_not_visible))
                                isVisible = false
                            }
                            else -> {
                                injector.visibility(R.id.error_icon, View.GONE)
                                injector.visibility(R.id.info_icon, View.VISIBLE)
                                injector.text(R.id.label, getString(R.string.settings_not_visible))
                                isVisible = false
                            }
                        }
                    }
                    Constants.GlanceProviderId.GREETINGS -> {
                        injector.text(
                            R.id.label,
                            if (Preferences.showGreetings) getString(R.string.settings_visible) else getString(
                                R.string.settings_not_visible
                            )
                        )
                        injector.visibility(R.id.error_icon, View.GONE)
                        injector.visibility(R.id.info_icon, View.VISIBLE)
                        isVisible = Preferences.showGreetings
                    }
                    Constants.GlanceProviderId.CUSTOM_INFO -> {
                        injector.text(
                            R.id.label,
                            if (Preferences.customNotes != "") getString(R.string.settings_visible) else getString(
                                R.string.settings_not_visible
                            )
                        )
                        injector.visibility(R.id.error_icon, View.GONE)
                        injector.visibility(R.id.info_icon, View.VISIBLE)
                        isVisible = Preferences.customNotes != ""
                    }
                    Constants.GlanceProviderId.EVENTS -> {
                        isVisible =
                            Preferences.showEventsAsGlanceProvider
                        val hasError = !Preferences.showEvents || !requireContext().checkGrantedPermission(
                            Manifest.permission.READ_CALENDAR
                        )
                        injector.text(
                            R.id.label,
                            if (isVisible && !hasError) getString(R.string.settings_visible) else getString(
                                R.string.settings_not_visible
                            )
                        )
                        injector.visibility(
                            R.id.error_icon,
                            if (isVisible && hasError) View.VISIBLE else View.GONE
                        )
                        injector.visibility(
                            R.id.info_icon,
                            if (!(isVisible && hasError)) View.VISIBLE else View.GONE
                        )
                    }
                }

                injector.alpha(R.id.title, if (isVisible) 1f else .25f)
                injector.alpha(R.id.label, if (isVisible) 1f else .25f)
                injector.alpha(R.id.icon, if (isVisible) 1f else .25f)
            }
            .attachTo(binding.providersList)

        val mIth = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                0
            ) {

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder,
                ): Boolean {
                    val fromPos = viewHolder.adapterPosition
                    val toPos = target.adapterPosition
                    // move item in `fromPos` to `toPos` in adapter.
                    adapter.notifyItemMoved(fromPos, toPos)
                    return true
                }

                override fun onMoved(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    fromPos: Int,
                    target: RecyclerView.ViewHolder,
                    toPos: Int,
                    x: Int,
                    y: Int
                ) {
                    with(list[toPos]) {
                        list[toPos] = list[fromPos]
                        list[fromPos] = this
                    }
                    super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
                }

                override fun isItemViewSwipeEnabled(): Boolean {
                    return false
                }

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ) {
                    super.clearView(recyclerView, viewHolder)
                    GlanceProviderHelper.saveGlanceProviderOrder(
                        list
                    )
                    adapter.updateData(list.mapNotNull {
                        GlanceProviderHelper.getGlanceProviderById(
                            requireContext(),
                            it
                        )
                    })
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean,
                ) {
                    val view = viewHolder.itemView as MaterialCardView
                    if (isCurrentlyActive) {
                        ViewCompat.setElevation(view, 8f.convertDpToPixel(requireContext()))
                        view.setCardBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.cardBorder
                            )
                        )
                    } else {
                        ViewCompat.setElevation(view, 0f)
                        view.setCardBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.colorPrimary
                            )
                        )
                    }

                    val topEdge =
                        if ((view.top == 0 && dY < 0) || ((view.top + view.height >= recyclerView.height - 32f.convertDpToPixel(
                                requireContext()
                            )) && dY > 0)
                        ) 0f else dY

                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        topEdge,
                        actionState,
                        isCurrentlyActive
                    )
                }

                override fun onSwiped(
                    viewHolder: RecyclerView.ViewHolder,
                    direction: Int,
                ) {
                    // remove from adapter
                }
            })

        mIth.attachToRecyclerView(binding.providersList)

        setupListener()

        binding.scrollView.viewTreeObserver.addOnScrollChangedListener {
            viewModel.fragmentScrollY.value = binding.scrollView.scrollY
        }

        lifecycleScope.launch(Dispatchers.IO) {
            delay(500)
            val l = list.mapNotNull { GlanceProviderHelper.getGlanceProviderById(
                requireContext(),
                it
            ) }
            withContext(Dispatchers.Main) {
                binding.loader.animate().scaleX(0f).scaleY(0f).alpha(0f).start()
                adapter.updateData(l)
                val controller =
                    AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)

                binding.providersList.layoutAnimation = controller
                adapter.notifyDataSetChanged()
                binding.providersList.scheduleLayoutAnimation()
            }
        }
    }

    private fun setupListener() {
    }

    private val nextAlarmChangeBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            adapter.notifyItemRangeChanged(0, adapter.data.size)
        }
    }

    override fun onStart() {
        super.onStart()
        requireActivity().registerReceiver(
            nextAlarmChangeBroadcastReceiver,
            IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        )
        if (dialog != null) {
            dialog?.show()
        }
    }

    override fun onStop() {
        requireActivity().unregisterReceiver(nextAlarmChangeBroadcastReceiver)
        super.onStop()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        when (requestCode) {
            1 -> {
                if (resultCode == Activity.RESULT_OK) {
                    adapter.notifyItemRangeChanged(0, adapter.data.size)
                } else {
                    Preferences.showDailySteps = false
                }

                if (dialog != null) {
                    dialog?.show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyItemRangeChanged(0, adapter.data?.size ?: 0)
        if (dialog != null) {
            dialog?.show()
        }
    }
}
