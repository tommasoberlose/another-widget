package com.tommasoberlose.anotherwidget.ui.activities.tabs

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chibatching.kotpref.blockingBulk
import com.google.gson.Gson
import com.koolio.library.Font
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.databinding.ActivityCustomFontBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.SettingsStringHelper
import com.tommasoberlose.anotherwidget.ui.viewmodels.tabs.CustomFontViewModel
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter
import net.idik.lib.slimadapter.diff.DefaultDiffCallback


class CustomFontActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: CustomFontViewModel
    private lateinit var binding: ActivityCustomFontBinding
    private lateinit var handlerThread: HandlerThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(CustomFontViewModel::class.java)
        binding = ActivityCustomFontBinding.inflate(layoutInflater)
        handlerThread = HandlerThread("listCustomFonts")
        handlerThread.start()

        binding.listView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        binding.listView.layoutManager = mLayoutManager

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
            .register<String>(R.layout.list_item) { item, injector ->
                injector
                    .text(R.id.text, item)
                    .with<TextView>(R.id.text) {
                        val googleSans: Typeface? = androidx.core.content.res.ResourcesCompat.getFont(
                            this,
                            when (Preferences.customFontVariant) {
                                "100" -> R.font.google_sans_thin
                                "200" -> R.font.google_sans_light
                                "500" -> R.font.google_sans_medium
                                "700" -> R.font.google_sans_bold
                                "800" -> R.font.google_sans_black
                                else -> R.font.google_sans_regular
                            }
                        )
                        it.typeface = googleSans
                    }

                injector.clicked(R.id.text) {
                    val dialog = BottomSheetMenu<String>(this, header = item)
                    listOf("100", "200", "regular", "500", "700", "800").forEachIndexed { _, s ->
                        dialog.addItem(SettingsStringHelper.getVariantLabel(this, s), s)
                    }
                    dialog.addOnSelectItemListener { value ->
                        saveGoogleSansFont(value)
                    }.show()
                }
            }
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
                                if (it.tag == this) {
                                    it.tag = null
                                    it.typeface = typeface
                                    it.setTextColor(getColor(R.color.colorPrimaryText))
                                }
                            }

                            override fun onTypefaceRequestFailed(reason: Int) {
                                if (it.tag == this) {
                                    it.tag = null
                                    //it.text = item.fontFamily + " ($reason)"
                                    it.setTextColor(getColor(R.color.errorColorText))
                                }
                            }
                        }

                        it.tag = callback;
                        it.typeface = null
                        it.setTextColor(getColor(R.color.colorSecondaryText))

                        val mHandler = Handler(handlerThread.looper)
                        FontsContractCompat.requestFont(this, request, callback, mHandler)
                    }

                injector.clicked(R.id.text) {
                    if ((it as TextView).typeface == null) return@clicked
                    val dialog = BottomSheetMenu<Int>(this, header = item.fontFamily)
                    if (item.fontVariants.isEmpty()) {
                        dialog.addItem(SettingsStringHelper.getVariantLabel(this, "regular"), -1)
                    } else {
                        item.fontVariants
                            .forEachIndexed { index, s ->
                                dialog.addItem(SettingsStringHelper.getVariantLabel(this, s), index)
                            }
                    }
                    dialog.addOnSelectItemListener { value ->
                        saveFont(item, value)
                    }.show()
                }
            }
            .attachTo(binding.listView)

        setupListener()
        subscribeUi(binding, viewModel)

        binding.search.requestFocus()

        setContentView(binding.root)
    }

    override fun onDestroy() {
        handlerThread.quit()
        filterJob?.cancel()
        super.onDestroy()
    }

    private var filterJob: Job? = null

    private fun subscribeUi(binding: ActivityCustomFontBinding, viewModel: CustomFontViewModel) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.fontList.observe(this, Observer {
            updateList(list = it)
            binding.loader.visibility = View.INVISIBLE
        })

        viewModel.searchInput.observe(this, Observer { search ->
            updateList(search = search)
            binding.clearSearch.isVisible = search.isNotBlank()
        })
    }

    private fun updateList(
        list: ArrayList<Font>? = viewModel.fontList.value,
        search: String? = viewModel.searchInput.value
    ) {
        binding.loader.visibility = View.VISIBLE
        filterJob?.cancel()
        filterJob = lifecycleScope.launch(Dispatchers.IO) {
            if (list != null && list.isNotEmpty()) {
                delay(200)
                val filteredList: List<Any> = if (search == null || search == "") {
                    listOf(getString(R.string.custom_font_subtitle_1)) + list.distinctBy { it.fontFamily }
                } else {
                    (listOf(getString(R.string.custom_font_subtitle_1)) + list.distinctBy { it.fontFamily }).filter {
                        when (it) {
                            is Font -> {
                                it.fontFamily.contains(search, true)
                            }
                            is String -> {
                                it.contains(search, ignoreCase = true)
                            }
                            else -> {
                                true
                            }
                        }
                    }
                }.sortedWith { el1, el2 ->
                    if (el1 is Font && el2 is Font) {
                        el1.fontFamily.compareTo(el2.fontFamily)
                    } else if (el1 is Font && el2 is String) {
                        el1.fontFamily.compareTo(el2)
                    } else if (el1 is String && el2 is Font) {
                        el1.compareTo(el2.fontFamily)
                    } else {
                        1
                    }
                }
                withContext(Dispatchers.Main) {
                    adapter.updateData(filteredList)
                    binding.loader.visibility = View.INVISIBLE
                }
            } else {
                delay(200)
                withContext(Dispatchers.Main) {
                    adapter.updateData(listOf(getString(R.string.custom_font_subtitle_1)).filter {
                        it.contains(search ?: "", ignoreCase = true)
                    })
                }
            }
        }
    }

    private fun setupListener() {
        binding.actionBack.setOnClickListener {
            onBackPressed()
        }

        binding.clearSearch.setOnClickListener {
            viewModel.searchInput.value = ""
        }
    }

    private fun saveFont(font: Font, variantPos: Int? = null) {
        val resultIntent = Intent()
        Preferences.blockingBulk {
            customFont = Constants.CUSTOM_FONT_DOWNLOADED
            customFontName = font.fontFamily
            customFontFile = if (variantPos != null && variantPos > -1) font.getQueryString(variantPos) else font.queryString
            customFontVariant = if (variantPos != null && variantPos > -1) font.fontVariants[variantPos] else "regular"
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun saveGoogleSansFont(variant: String) {
        val resultIntent = Intent()
        Preferences.blockingBulk {
            customFont = Constants.CUSTOM_FONT_GOOGLE_SANS
            customFontName = ""
            customFontFile = ""
            customFontVariant = variant
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
