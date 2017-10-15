package com.tommasoberlose.anotherwidget.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.transition.Slide
import android.view.Gravity
import com.tommasoberlose.anotherwidget.R
import android.support.v4.view.ViewCompat.setAlpha
import android.support.v4.view.ViewCompat.animate
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.`object`.CustomLocationEvent
import com.tommasoberlose.anotherwidget.util.WeatherUtil
import kotlinx.android.synthetic.main.activity_custom_location.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.Subscribe






class CustomLocationActivity : AppCompatActivity() {
    lateinit var adapter: ArrayAdapter<String>
    val addressesList = ArrayList<Address>()

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_location)

        val SP = PreferenceManager.getDefaultSharedPreferences(this)

        adapter = ArrayAdapter(this, R.layout.custom_location_item, addressesList.map { it.getAddressLine(0) })
        list_view.adapter = adapter

        list_view.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            SP.edit()
                    .putString(Constants.PREF_CUSTOM_LOCATION_LAT, addressesList[position].latitude.toString())
                    .putString(Constants.PREF_CUSTOM_LOCATION_LON, addressesList[position].longitude.toString())
                    .putString(Constants.PREF_CUSTOM_LOCATION_ADD, addressesList[position].getAddressLine(0))
                    .commit()
            setResult(Activity.RESULT_OK)
            finish()
        }

        action_geolocation.setOnClickListener {
            SP.edit()
                    .remove(Constants.PREF_CUSTOM_LOCATION_LAT)
                    .remove(Constants.PREF_CUSTOM_LOCATION_LON)
                    .remove(Constants.PREF_CUSTOM_LOCATION_ADD)
                    .commit()
            setResult(Activity.RESULT_OK)
            finish()
        }

        location.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(text: Editable?) {
                Thread().run {
                    val coder = Geocoder(this@CustomLocationActivity)
                    try {
                        val addresses = coder.getFromLocationName(text.toString(), 10) as ArrayList<Address>
                        EventBus.getDefault().post(CustomLocationEvent(addresses))
                    } catch (ignored: Exception) {
                        EventBus.getDefault().post(CustomLocationEvent(ArrayList<Address>()))
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

    }

    public override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    public override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onMessageEvent(event: CustomLocationEvent) {
        adapter.clear()
        addressesList.clear()
        event.addresses.mapTo(addressesList, {it})
        for (a:Address in addressesList) {
            adapter.add(a.getAddressLine(0))
        }
        adapter.notifyDataSetChanged()
    }
}
