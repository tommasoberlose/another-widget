package com.tommasoberlose.anotherwidget.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import com.tommasoberlose.anotherwidget.R
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chibatching.kotpref.bulk
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tommasoberlose.anotherwidget.components.MaterialBottomSheetDialog
import com.tommasoberlose.anotherwidget.databinding.ActivityChooseApplicationBinding
import com.tommasoberlose.anotherwidget.databinding.ActivityCustomLocationBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.viewmodels.ChooseApplicationViewModel
import com.tommasoberlose.anotherwidget.ui.viewmodels.CustomLocationViewModel
import kotlinx.android.synthetic.main.activity_custom_location.*
import kotlinx.android.synthetic.main.activity_custom_location.action_back
import kotlinx.android.synthetic.main.activity_custom_location.clear_search
import kotlinx.android.synthetic.main.activity_custom_location.list_view
import kotlinx.android.synthetic.main.activity_custom_location.loader
import kotlinx.android.synthetic.main.activity_music_players_filter.*
import kotlinx.coroutines.*
import net.idik.lib.slimadapter.SlimAdapter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.Subscribe

class CustomLocationActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: CustomLocationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(CustomLocationViewModel::class.java)
        val binding = DataBindingUtil.setContentView<ActivityCustomLocationBinding>(this, R.layout.activity_custom_location)


        list_view.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        list_view.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<String>(R.layout.custom_location_item) { _, injector ->
                injector
                    .text(R.id.text, getString(R.string.custom_location_gps))
                    .clicked(R.id.item) {
                        MaterialBottomSheetDialog(this, message = getString(R.string.background_location_warning))
                            .setPositiveButton(getString(android.R.string.ok)) {
                                requirePermission()
                            }
                            .show()
                    }
            }
            .register<Address>(R.layout.custom_location_item) { item, injector ->
                injector.text(R.id.text, item.getAddressLine(0))
                injector.clicked(R.id.item) {
                    Preferences.bulk {
                        customLocationLat = item.latitude.toString()
                        customLocationLon = item.longitude.toString()
                        customLocationAdd = item.getAddressLine(0)
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            }
            .attachTo(list_view)


        viewModel.addresses.observe(this, Observer {
            adapter.updateData(listOf("Default") + it)
        })

        setupListener()
        subscribeUi(binding, viewModel)

        location.requestFocus()

    }
    private var searchJob: Job? = null

    private fun subscribeUi(binding: ActivityCustomLocationBinding, viewModel: CustomLocationViewModel) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.addresses.observe(this, Observer {
            adapter.updateData(listOf("Default") + it)
            loader.visibility = View.INVISIBLE
        })

        viewModel.locationInput.observe(this, Observer { location ->
            loader.visibility = View.VISIBLE
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
                        emptyList<Address>()
                    }
                }
                withContext(Dispatchers.Main) {
                    viewModel.addresses.value = list
                    loader.visibility = View.INVISIBLE
                }

            }
            clear_search.isVisible = location.isNotBlank()
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
        action_back.setOnClickListener {
            onBackPressed()
        }

        clear_search.setOnClickListener {
            viewModel.locationInput.value = ""
        }
    }
}
