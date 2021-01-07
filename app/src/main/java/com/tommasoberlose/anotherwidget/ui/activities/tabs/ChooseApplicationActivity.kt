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
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.tommasoberlose.anotherwidget.databinding.ActivityChooseApplicationBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.ui.viewmodels.ChooseApplicationViewModel
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter
import net.idik.lib.slimadapter.SlimAdapterEx


class ChooseApplicationActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: ChooseApplicationViewModel
    private lateinit var binding: ActivityChooseApplicationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(ChooseApplicationViewModel::class.java)
        binding = ActivityChooseApplicationBinding.inflate(layoutInflater)

        binding.listView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        binding.listView.layoutManager = mLayoutManager

        adapter = SlimAdapterEx.create()
        adapter
            .register<String>(R.layout.application_info_layout) { _, injector ->
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
                    resultIntent.putExtra(Constants.RESULT_APP_NAME, "")
                    resultIntent.putExtra(Constants.RESULT_APP_PACKAGE, "")
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
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

                injector.clicked(R.id.item) {
                    saveApp(item)
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

        viewModel.appList.observe(this, Observer {
            updateList(list = it)
            binding.loader.visibility = View.INVISIBLE
        })

        viewModel.searchInput.observe(this, Observer { search ->
            updateList(search = search)
            binding.clearSearch.isVisible = search.isNotBlank()
        })
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
                }
                withContext(Dispatchers.Main) {
                    adapter.updateData(listOf("Default") + filteredList)
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
