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
import android.widget.Toast
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.util.WeatherUtil
import kotlinx.android.synthetic.main.activity_custom_location.*


class CustomLocationActivity : AppCompatActivity() {

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_location)

        val SP = PreferenceManager.getDefaultSharedPreferences(this)

        val list = ArrayList<String>()
        val addressesList = ArrayList<Address>()
        val thread: Thread = Thread()

        var adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
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

        location.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(text: Editable?) {
                val coder = Geocoder(this@CustomLocationActivity)
                try {
                    val addresses = coder.getFromLocationName(text.toString(), 10) as ArrayList<Address>
                    list.clear()
                    addressesList.clear()

                    addresses.mapTo(list) { it.getAddressLine(0) }
                    addresses.mapTo(addressesList) { it }

                    adapter = ArrayAdapter(this@CustomLocationActivity, R.layout.custom_location_item, list)
                    list_view.adapter = adapter
                } catch (ignored: Exception) {
                    Toast.makeText(this@CustomLocationActivity, "Erroreeeee", Toast.LENGTH_SHORT).show()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

    }
}
