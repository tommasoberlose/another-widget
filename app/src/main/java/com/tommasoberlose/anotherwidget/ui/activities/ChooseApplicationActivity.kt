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
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.tommasoberlose.anotherwidget.databinding.ActivityChooseApplicationBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.ui.viewmodels.ChooseApplicationViewModel
import kotlinx.android.synthetic.main.activity_choose_application.*
import kotlinx.android.synthetic.main.activity_choose_application.list_view
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter


class ChooseApplicationActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: ChooseApplicationViewModel
    private val pm by lazy { packageManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(ChooseApplicationViewModel::class.java)
        val binding = DataBindingUtil.setContentView<ActivityChooseApplicationBinding>(this, R.layout.activity_choose_application)

        list_view.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        list_view.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<String>(R.layout.application_info_layout) { _, injector ->
                injector
                    .text(R.id.text, getString(R.string.default_name))
                    .image(R.id.icon, R.drawable.round_add_to_home_screen)
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
                    .text(R.id.text, item.loadLabel(pm))
                    .with<ImageView>(R.id.icon) {
                        Glide
                            .with(this)
                            .load(item.loadIcon(pm))
                            .centerCrop()
                            .into(it)
                    }

                injector.clicked(R.id.item) {
                    saveApp(item)
                }
            }
            .attachTo(list_view)

        setupListener()
        subscribeUi(binding, viewModel)

        search.requestFocus()
    }

    private var filterJob: Job? = null

    private fun subscribeUi(binding: ActivityChooseApplicationBinding, viewModel: ChooseApplicationViewModel) {
        binding.viewModel = viewModel
        viewModel.appList.observe(this, Observer {
            adapter.updateData(listOf("Default") + it)
            loader.visibility = View.INVISIBLE
        })

        viewModel.searchInput.observe(this, Observer { search ->
            loader.visibility = View.VISIBLE
            filterJob?.cancel()
            filterJob = lifecycleScope.launch(Dispatchers.IO) {
                delay(200)
                val list = if (search == null || search == "") {
                    viewModel.appList.value!!
                } else {
                    viewModel.appList.value!!.filter {
                        it.loadLabel(pm).contains(search, true)
                    }
                }
                withContext(Dispatchers.Main) {
                    adapter.updateData(listOf("Default") + list)
                    loader.visibility = View.INVISIBLE
                }

            }
        })

//        viewModel.filterSettingsApp.observe(this, Observer {
//            action_filter.alpha = if (it) 1f else 0.5f
//        })
    }

    private fun setupListener() {
        action_back.setOnClickListener {
            onBackPressed()
        }

//        action_filter.setOnClickListener {
//            viewModel.toggleFilter()
//        }
    }

    private fun saveApp(app: ResolveInfo) {
        val resultIntent = Intent()
        resultIntent.putExtra(Constants.RESULT_APP_NAME, app.loadLabel(pm))
        resultIntent.putExtra(Constants.RESULT_APP_PACKAGE, app.activityInfo.packageName)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
