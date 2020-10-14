package com.tommasoberlose.anotherwidget.ui.activities

import android.app.Activity
import android.os.Bundle
import com.tommasoberlose.anotherwidget.R
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.tommasoberlose.anotherwidget.databinding.ActivityChooseApplicationBinding
import com.tommasoberlose.anotherwidget.databinding.ActivityMusicPlayersFilterBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.ui.viewmodels.ChooseApplicationViewModel
import com.tommasoberlose.anotherwidget.ui.viewmodels.MusicPlayersFilterViewModel
import kotlinx.android.synthetic.main.activity_music_players_filter.*
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter
import kotlin.Comparator as Comparator1


class MusicPlayersFilterActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: MusicPlayersFilterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(MusicPlayersFilterViewModel::class.java)
        val binding = DataBindingUtil.setContentView<ActivityMusicPlayersFilterBinding>(this, R.layout.activity_music_players_filter)

        list_view.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        list_view.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<ResolveInfo>(R.layout.application_info_layout) { item, injector ->
                injector
                    .text(R.id.text, item.loadLabel(viewModel.pm))
                    .with<ImageView>(R.id.icon) {
                        Glide
                            .with(this)
                            .load(item.loadIcon(viewModel.pm))
                            .centerCrop()
                            .into(it)
                    }
                    .visible(R.id.checkBox)
                    .clicked(R.id.item) {
                        toggleApp(item)
                        adapter.notifyItemRangeChanged(0, adapter.data.size)
                    }
                    .clicked(R.id.checkBox) {
                        toggleApp(item)
                        adapter.notifyItemRangeChanged(0, adapter.data.size)
                    }
                    .checked(R.id.checkBox, MediaPlayerHelper.isMusicPlayerAccepted(item.activityInfo.packageName))
            }
            .attachTo(list_view)

        setupListener()
        subscribeUi(binding, viewModel)

        search.requestFocus()
    }

    private var filterJob: Job? = null

    private fun subscribeUi(binding: ActivityMusicPlayersFilterBinding, viewModel: MusicPlayersFilterViewModel) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.appList.observe(this, Observer {
            updateList(list = it)
            loader.visibility = View.INVISIBLE
        })

        viewModel.searchInput.observe(this, Observer { search ->
            updateList(search = search)
            clear_search.isVisible = search.isNotBlank()
        })

        viewModel.musicPlayersFilter.observe(this, {
            updateList()
            clear_selection.isVisible = Preferences.musicPlayersFilter != ""
        })
    }

    private fun updateList(list: List<ResolveInfo>? = viewModel.appList.value, search: String? = viewModel.searchInput.value) {
        loader.visibility = View.VISIBLE
        filterJob?.cancel()
        filterJob = lifecycleScope.launch(Dispatchers.IO) {
            if (list != null && list.isNotEmpty()) {
                delay(200)
                val filteredList: List<ResolveInfo> = if (search == null || search == "") {
                    list
                } else {
                    list.filter {
                        it.loadLabel(viewModel.pm).contains(search, true)
                    }
                }.sortedWith { app1, app2 ->
                    if (MediaPlayerHelper.isMusicPlayerAccepted(app1.activityInfo.packageName) && MediaPlayerHelper.isMusicPlayerAccepted(app2.activityInfo.packageName)) {
                        app1.loadLabel(viewModel.pm).toString().compareTo(app2.loadLabel(viewModel.pm).toString(), ignoreCase = true)
                    } else if (MediaPlayerHelper.isMusicPlayerAccepted(app1.activityInfo.packageName)) {
                        -1
                    } else if (MediaPlayerHelper.isMusicPlayerAccepted(app2.activityInfo.packageName)) {
                        1
                    } else {
                        app1.loadLabel(viewModel.pm).toString().compareTo(app2.loadLabel(viewModel.pm).toString(), ignoreCase = true)
                    }
                }


                withContext(Dispatchers.Main) {
                    adapter.updateData(filteredList)
                    loader.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun setupListener() {
        action_back.setOnClickListener {
            onBackPressed()
        }

        clear_search.setOnClickListener {
            viewModel.searchInput.value = ""
        }

        clear_selection.setOnClickListener {
            Preferences.musicPlayersFilter = ","
        }
    }

    private fun toggleApp(app: ResolveInfo) {
        MediaPlayerHelper.toggleMusicPlayerFilter(app.activityInfo.packageName)
    }
}
