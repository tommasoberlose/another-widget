package com.tommasoberlose.anotherwidget.ui.activities.tabs

import android.app.Activity
import android.os.Bundle
import com.tommasoberlose.anotherwidget.R
import android.content.Intent
import android.content.pm.ResolveInfo
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.tommasoberlose.anotherwidget.databinding.ActivityChooseApplicationBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.helpers.IntentHelper
import com.tommasoberlose.anotherwidget.ui.viewmodels.tabs.ChooseApplicationViewModel
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter
import net.idik.lib.slimadapter.SlimAdapterEx


class ChooseApplicationActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: ChooseApplicationViewModel
    private lateinit var binding: ActivityChooseApplicationBinding

    private var selectedPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedPackage = intent.extras?.getString(Constants.RESULT_APP_PACKAGE)

        viewModel = ViewModelProvider(this).get(ChooseApplicationViewModel::class.java)
        binding = ActivityChooseApplicationBinding.inflate(layoutInflater)

        binding.listView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        binding.listView.layoutManager = mLayoutManager

        adapter = SlimAdapterEx.create()
        adapter
            .register<String>(R.layout.application_info_layout) { item, injector ->
                when (item) {
                    IntentHelper.DO_NOTHING_OPTION -> {
                        injector
                            .text(R.id.text, getString(R.string.gestures_do_nothing))
                            .image(R.id.icon, R.drawable.round_no_cell_24)
                            .with<ImageView>(R.id.icon) {
                                it.scaleX = 0.8f
                                it.scaleY = 0.8f
                                it.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryText), android.graphics.PorterDuff.Mode.MULTIPLY)
                            }
                            .clicked(R.id.item) {
                                val resultIntent = Intent()
                                resultIntent.putExtra(Constants.RESULT_APP_NAME, IntentHelper.DO_NOTHING_OPTION)
                                resultIntent.putExtra(Constants.RESULT_APP_PACKAGE, IntentHelper.DO_NOTHING_OPTION)
                                setResult(Activity.RESULT_OK, resultIntent)
                                finish()
                            }
                            .with<MaterialCardView>(R.id.item) {
                                it.strokeColor = ContextCompat.getColor(this, if (selectedPackage == IntentHelper.DO_NOTHING_OPTION) R.color.colorAccent else R.color.cardBorder)
                                it.setCardBackgroundColor(ContextCompat.getColor(this, if (selectedPackage == IntentHelper.DO_NOTHING_OPTION) R.color.colorAccent_op10 else R.color.colorPrimaryDark))
                            }
                    }
                    IntentHelper.REFRESH_WIDGET_OPTION -> {
                        injector
                            .text(R.id.text, getString(R.string.action_refresh_widget))
                            .image(R.id.icon, R.drawable.round_refresh)
                            .with<ImageView>(R.id.icon) {
                                it.scaleX = 0.8f
                                it.scaleY = 0.8f
                                it.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryText), android.graphics.PorterDuff.Mode.MULTIPLY)
                            }
                            .clicked(R.id.item) {
                                val resultIntent = Intent()
                                resultIntent.putExtra(Constants.RESULT_APP_NAME, IntentHelper.REFRESH_WIDGET_OPTION)
                                resultIntent.putExtra(Constants.RESULT_APP_PACKAGE, IntentHelper.REFRESH_WIDGET_OPTION)
                                setResult(Activity.RESULT_OK, resultIntent)
                                finish()
                            }
                            .with<MaterialCardView>(R.id.item) {
                                it.strokeColor = ContextCompat.getColor(this, if (selectedPackage == IntentHelper.REFRESH_WIDGET_OPTION) R.color.colorAccent else R.color.cardBorder)
                                it.setCardBackgroundColor(ContextCompat.getColor(this, if (selectedPackage == IntentHelper.REFRESH_WIDGET_OPTION) R.color.colorAccent_op10 else R.color.colorPrimaryDark))
                            }
                    }
                    else -> {
                        injector
                            .text(R.id.text, getString(R.string.default_name))
                            .image(R.id.icon, R.drawable.round_add_to_home_screen_24)
                            .with<ImageView>(R.id.icon) {
                                it.scaleX = 0.8f
                                it.scaleY = 0.8f
                                it.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryText), android.graphics.PorterDuff.Mode.MULTIPLY)
                            }
                            .clicked(R.id.item) {
                                val resultIntent = Intent()
                                resultIntent.putExtra(Constants.RESULT_APP_NAME, IntentHelper.DEFAULT_OPTION)
                                resultIntent.putExtra(Constants.RESULT_APP_PACKAGE, IntentHelper.DEFAULT_OPTION)
                                setResult(Activity.RESULT_OK, resultIntent)
                                finish()
                            }
                            .with<MaterialCardView>(R.id.item) {
                                it.strokeColor = ContextCompat.getColor(this, if (selectedPackage == IntentHelper.DEFAULT_OPTION) R.color.colorAccent else R.color.cardBorder)
                                it.setCardBackgroundColor(ContextCompat.getColor(this, if (selectedPackage == IntentHelper.DEFAULT_OPTION) R.color.colorAccent_op10 else R.color.colorPrimaryDark))
                            }
                    }
                }
            }
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
                    .clicked(R.id.item) {
                        saveApp(item)
                    }
                    .with<MaterialCardView>(R.id.item) {
                        it.strokeColor = ContextCompat.getColor(this, if (selectedPackage == item.activityInfo.packageName) R.color.colorAccent else R.color.cardBorder)
                        it.setCardBackgroundColor(ContextCompat.getColor(this, if (selectedPackage == item.activityInfo.packageName) R.color.colorAccent_op10 else R.color.colorPrimaryDark))
                    }
            }
            .attachTo(binding.listView)

        setupListener()
        subscribeUi(binding, viewModel)

        binding.search.requestFocus()

        setContentView(binding.root)
    }

    private var filterJob: Job? = null

    private fun subscribeUi(binding: ActivityChooseApplicationBinding, viewModel: ChooseApplicationViewModel) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.appList.observe(this) {
            updateList(list = it)
            binding.loader.visibility = View.INVISIBLE
        }

        viewModel.searchInput.observe(this) { search ->
            updateList(search = search)
            binding.clearSearch.isVisible = search.isNotBlank()
        }
    }

    private fun updateList(list: List<ResolveInfo>? = viewModel.appList.value, search: String? = viewModel.searchInput.value) {
        binding.loader.visibility = View.VISIBLE
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
                    when (selectedPackage) {
                        app1.activityInfo.packageName -> {
                            -1
                        }
                        app2.activityInfo.packageName -> {
                            1
                        }
                        else -> {
                            app1.loadLabel(viewModel.pm).toString().compareTo(app2.loadLabel(viewModel.pm).toString(), ignoreCase = true)
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    adapter.updateData(listOf(IntentHelper.DO_NOTHING_OPTION, IntentHelper.DEFAULT_OPTION, IntentHelper.REFRESH_WIDGET_OPTION) + filteredList)
                    binding.loader.visibility = View.INVISIBLE
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

    private fun saveApp(app: ResolveInfo) {
        val resultIntent = Intent()
        resultIntent.putExtra(Constants.RESULT_APP_NAME, app.loadLabel(viewModel.pm))
        resultIntent.putExtra(Constants.RESULT_APP_PACKAGE, app.activityInfo.packageName)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
