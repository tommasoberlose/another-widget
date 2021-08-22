package com.tommasoberlose.anotherwidget.ui.activities.tabs

import android.app.Activity
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivityTimeZoneSelectorBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.network.TimeZonesApi
import com.tommasoberlose.anotherwidget.ui.viewmodels.tabs.TimeZoneSelectorViewModel
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.toast
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter

class TimeZoneSelectorActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: TimeZoneSelectorViewModel
    private lateinit var binding: ActivityTimeZoneSelectorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(TimeZoneSelectorViewModel::class.java)
        binding = ActivityTimeZoneSelectorBinding.inflate(layoutInflater)

        binding.geonameCredits.movementMethod = LinkMovementMethod.getInstance()

        binding.listView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        binding.listView.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<String>(R.layout.custom_location_item) { _, injector ->
                injector
                    .text(R.id.text, getString(R.string.no_time_zone_label))
                    .clicked(R.id.text) {
                        Preferences.bulk {
                            altTimezoneId = ""
                            altTimezoneLabel = ""
                        }
                        MainWidget.updateWidget(this@TimeZoneSelectorActivity)
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
            }
            .register<Address>(R.layout.custom_location_item) { item, injector ->
                injector.text(R.id.text, item.getAddressLine(0))
                injector.clicked(R.id.item) {
                    binding.loader.visibility = View.VISIBLE
                    lifecycleScope.launch(Dispatchers.IO) {
                        val networkApi = TimeZonesApi(this@TimeZoneSelectorActivity)
                        val id = networkApi.getTimeZone(item.latitude.toString(), item.longitude.toString())

                        if (id != null) {
                            Preferences.bulk {
                                altTimezoneId = id
                                altTimezoneLabel = try {
                                    item.locality
                                } catch (ex: Exception) {
                                    item.getAddressLine(0)
                                }
                            }
                            MainWidget.updateWidget(this@TimeZoneSelectorActivity)
                            setResult(Activity.RESULT_OK)
                            finish()
                        } else {
                            withContext(Dispatchers.Main) {
                                binding.loader.visibility = View.INVISIBLE
                                toast(getString(R.string.time_zone_search_error_message))
                            }
                        }
                    }
                }
            }
            .attachTo(binding.listView)


        viewModel.addresses.observe(this, {
            adapter.updateData(listOf("Default") + it)
        })

        setupListener()
        subscribeUi(binding, viewModel)

        binding.location.requestFocus()

        setContentView(binding.root)
    }

    private var searchJob: Job? = null

    private fun subscribeUi(binding: ActivityTimeZoneSelectorBinding, viewModel: TimeZoneSelectorViewModel) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.addresses.observe(this, {
            adapter.updateData(listOf("Default") + it)
            binding.loader.visibility = View.INVISIBLE
        })

        viewModel.locationInput.observe(this, { location ->
            binding.loader.visibility = View.VISIBLE
            searchJob?.cancel()
            searchJob = lifecycleScope.launch(Dispatchers.IO) {
                delay(200)
                val list = if (location == null || location == "") {
                    viewModel.addresses.value!!
                } else {
                    val coder = Geocoder(this@TimeZoneSelectorActivity)
                    try {
                        coder.getFromLocationName(location, 10) as ArrayList<Address>
                    } catch (ignored: Exception) {
                        emptyList()
                    }
                }
                withContext(Dispatchers.Main) {
                    viewModel.addresses.value = list
                    binding.loader.visibility = View.INVISIBLE
                }

            }
            binding.clearSearch.isVisible = location.isNotBlank()
        })
    }

    private fun setupListener() {
        binding.actionBack.setOnClickListener {
            onBackPressed()
        }

        binding.clearSearch.setOnClickListener {
            viewModel.locationInput.value = ""
        }
    }
}
