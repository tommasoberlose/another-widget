package com.tommasoberlose.anotherwidget.ui.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chibatching.kotpref.blockingBulk
import com.koolio.library.Font
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivityCustomFontBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.viewmodels.CustomFontViewModel
import kotlinx.android.synthetic.main.activity_choose_application.*
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter
import net.idik.lib.slimadapter.diff.DefaultDiffCallback


class CustomFontActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: CustomFontViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(CustomFontViewModel::class.java)
        val binding = DataBindingUtil.setContentView<ActivityCustomFontBinding>(
            this,
            R.layout.activity_custom_font
        )

        list_view.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        list_view.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter.enableDiff(object: DefaultDiffCallback() {
            override fun areItemsTheSame(oldItem: Any?, newItem: Any?): Boolean {
                return oldItem is Font && newItem is Font && oldItem.fontFamily == newItem.fontFamily
            }

            override fun areContentsTheSame(oldItem: Any?, newItem: Any?): Boolean {
                return oldItem is Font && newItem is Font && oldItem.fontFamily == newItem.fontFamily
            }
        })
        adapter
            .register<Font>(R.layout.list_item) { item, injector ->
                injector
                    .text(R.id.text, item.fontFamily)
                    .with<TextView>(R.id.text) {
                        val request = FontRequest(
                            "com.google.android.gms.fonts",
                            "com.google.android.gms",
                            item.queryString,
                            R.array.com_google_android_gms_fonts_certs
                        )


                        val callback = object : FontsContractCompat.FontRequestCallback() {
                            override fun onTypefaceRetrieved(typeface: Typeface) {
                                it.typeface = typeface
                                it.isVisible = true

                                it.measure(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            }

                            override fun onTypefaceRequestFailed(reason: Int) {
                                it.isVisible = false
                                it.layoutParams = it.layoutParams.apply {
                                    height = 0
                                }
                            }
                        }

                        val handlerThread = HandlerThread(item.fontFamily)
                        handlerThread.start()
                        val mHandler = Handler(handlerThread.looper)
                        FontsContractCompat.requestFont(this, request, callback, mHandler)
                    }

                injector.clicked(R.id.text) {
                    saveFont(item)
                }
            }
            .attachTo(list_view)

        setupListener()
        subscribeUi(binding, viewModel)

        search.requestFocus()
    }

    private var filterJob: Job? = null

    private fun subscribeUi(binding: ActivityCustomFontBinding, viewModel: CustomFontViewModel) {
        binding.viewModel = viewModel

        viewModel.fontList.observe(this, Observer {
            updateList(list = it)
            loader.visibility = View.INVISIBLE
        })

        viewModel.searchInput.observe(this, Observer { search ->
            updateList(search = search)
        })
    }

    private fun updateList(
        list: ArrayList<Font>? = viewModel.fontList.value,
        search: String? = viewModel.searchInput.value
    ) {
        loader.visibility = View.VISIBLE
        filterJob?.cancel()
        filterJob = lifecycleScope.launch(Dispatchers.IO) {
            if (list != null && list.isNotEmpty()) {
                delay(200)
                val filteredList: List<Font> = if (search == null || search == "") {
                    list
                } else {
                    list.filter {
                        it.fontFamily.contains(search, true)
                    }
                }.distinctBy { it.fontFamily }
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
    }

    private fun saveFont(font: Font) {
        val resultIntent = Intent()
        Preferences.blockingBulk {
            customFont = Constants.CUSTOM_FONT_DOWNLOADED
            customFontName = font.fontFamily
            customFontFile = font.queryString
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
