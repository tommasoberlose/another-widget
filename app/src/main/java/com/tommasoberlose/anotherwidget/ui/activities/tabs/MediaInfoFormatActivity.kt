package com.tommasoberlose.anotherwidget.ui.activities.tabs

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chibatching.kotpref.blockingBulk
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivityMediaInfoFormatBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.ui.viewmodels.tabs.MediaInfoFormatViewModel
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter

class MediaInfoFormatActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: MediaInfoFormatViewModel
    private lateinit var binding: ActivityMediaInfoFormatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(MediaInfoFormatViewModel::class.java)
        binding = ActivityMediaInfoFormatBinding.inflate(layoutInflater)


        binding.listView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        binding.listView.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<String>(R.layout.custom_date_example_item) { item, injector ->
                injector
                    .text(R.id.custom_date_example_format, item)
                    .text(
                        R.id.custom_date_example_value, MediaPlayerHelper.getMediaInfo(item, EXAMPLE_TITLE, EXAMPLE_ARTIST, EXAMPLE_ALBUM))
            }
            .attachTo(binding.listView)

        adapter.updateData(
            listOf(
                MediaPlayerHelper.MEDIA_INFO_TITLE, MediaPlayerHelper.MEDIA_INFO_ARTIST, MediaPlayerHelper.MEDIA_INFO_ALBUM
            )
        )

        setupListener()
        subscribeUi(binding, viewModel)

        binding.mediaInfoFormatInput.requestFocus()

        setContentView(binding.root)
    }

    private var formatJob: Job? = null

    private fun subscribeUi(binding: ActivityMediaInfoFormatBinding, viewModel: MediaInfoFormatViewModel) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.mediaInfoFormatInput.observe(this) { mediaInfoFormatInput ->
            formatJob?.cancel()
            formatJob = lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    binding.loader.visibility = View.VISIBLE
                }

                delay(200)
                val text = MediaPlayerHelper.getMediaInfo(mediaInfoFormatInput, EXAMPLE_TITLE, EXAMPLE_ARTIST, EXAMPLE_ALBUM)

                withContext(Dispatchers.Main) {
                    binding.loader.visibility = View.INVISIBLE
                    binding.mediaInfoFormatInputValue.text = text
                }

            }
        }
    }

    private fun setupListener() {
        binding.actionBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        Preferences.blockingBulk {
            mediaInfoFormat = viewModel.mediaInfoFormatInput.value ?: ""
        }
        super.onBackPressed()
    }

    companion object {
        const val EXAMPLE_TITLE = "Thunderstruck"
        const val EXAMPLE_ARTIST = "AC/DC"
        const val EXAMPLE_ALBUM = "The Razors Edge"
    }
}
