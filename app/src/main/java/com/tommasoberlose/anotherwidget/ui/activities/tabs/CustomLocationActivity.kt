package com.tommasoberlose.anotherwidget.ui.activities.tabs

import android.Manifest
import android.app.Activity
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import com.tommasoberlose.anotherwidget.R
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chibatching.kotpref.bulk
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tommasoberlose.anotherwidget.databinding.ActivityCustomLocationBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.viewmodels.tabs.CustomLocationViewModel
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter

class CustomLocationActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: CustomLocationViewModel
    private lateinit var binding: ActivityCustomLocationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(CustomLocationViewModel::class.java)
        binding = ActivityCustomLocationBinding.inflate(layoutInflater)


        binding.listView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        binding.listView.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<String>(R.layout.custom_location_item) { _, injector ->
                injector
                    .text(R.id.text, getString(R.string.custom_location_gps))
                    .clicked(R.id.text) {
                        requirePermission()
                    }
            }
            .register<Address>(R.layout.custom_location_item) { item, injector ->
                injector.text(R.id.text, item.getAddressLine(0) ?: "")
                injector.clicked(R.id.item) {
                    Preferences.bulk {
                        customLocationLat = item.latitude.toString()
                        customLocationLon = item.longitude.toString()
                        customLocationAdd = item.getAddressLine(0) ?: ""
                        setResult(Activity.RESULT_OK)
                        finish()
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

    private fun subscribeUi(binding: ActivityCustomLocationBinding, viewModel: CustomLocationViewModel) {
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
                    val coder = Geocoder(this@CustomLocationActivity)
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

    private fun requirePermission() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION
            ).withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if (report.areAllPermissionsGranted()){
                            Preferences.bulk {
                                remove(Preferences::customLocationLat)
                                remove(Preferences::customLocationLon)
                                remove(Preferences::customLocationAdd)
                            }
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    // Remember to invoke this method when the custom rationale is closed
                    // or just by default if you don't want to use any custom rationale.
                    token?.continuePermissionRequest()
                }
            })
            .check()
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
