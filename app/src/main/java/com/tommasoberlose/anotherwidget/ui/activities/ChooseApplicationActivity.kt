package com.tommasoberlose.anotherwidget.ui.activities

import android.app.Activity
import android.os.Bundle
import com.tommasoberlose.anotherwidget.R
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.tommasoberlose.anotherwidget.components.events.ApplicationListEvent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialFadeThrough
import com.tommasoberlose.anotherwidget.components.events.AppInfoSavedEvent
import com.tommasoberlose.anotherwidget.databinding.ActivityChooseApplicationBinding
import com.tommasoberlose.anotherwidget.databinding.FragmentClockSettingsBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.ui.viewmodels.ChooseApplicationViewModel
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.toast
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
            .register<ApplicationInfo>(R.layout.application_info_layout) { item, injector ->
                injector
                    .text(R.id.text, pm.getApplicationLabel(item).toString())

                try {
                    injector.image(R.id.icon, item.loadIcon(pm))
                } catch (ignore: Exception) {
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
                        pm.getApplicationLabel(
                            it
                        ).toString().contains(search, true)
                    }
                }
                withContext(Dispatchers.Main) {
                    adapter.updateData(listOf("Default") + list)
                    loader.visibility = View.INVISIBLE
                }

            }
        })
    }

    private fun setupListener() {
        action_back.setOnClickListener {
            onBackPressed()
        }
    }

    private fun saveApp(app: ApplicationInfo) {
        val resultIntent = Intent()
        resultIntent.putExtra(Constants.RESULT_APP_NAME, pm.getApplicationLabel(app).toString())
        resultIntent.putExtra(Constants.RESULT_APP_PACKAGE, app.packageName)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
