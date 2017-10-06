package com.tommasoberlose.anotherwidget.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.ComponentName
import android.view.View
import android.widget.Toast
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.util.Util
import com.tommasoberlose.anotherwidget.ui.widget.TheWidget
import com.tommasoberlose.anotherwidget.util.UpdatesReceiver
import com.tommasoberlose.anotherwidget.util.WeatherReceiver
import kotlinx.android.synthetic.main.activity_main.*




class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        action_support.setOnClickListener(object: View.OnClickListener {
            override fun onClick(p0: View?) {
                Util.openURI(this@MainActivity, "https://paypal.me/tommasoberlose")
            }
        })

        action_rate.setOnClickListener(object: View.OnClickListener {
            override fun onClick(p0: View?) {
                Util.openURI(this@MainActivity, "")
            }
        })

        action_share.setOnClickListener(object: View.OnClickListener {
            override fun onClick(p0: View?) {
                Util.share(this@MainActivity)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        Util.updateWidget(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                   grantResults: IntArray) {
        when (requestCode) {
            Constants.CALENDAR_REQUEST_CODE -> if (permissions.size != 1 || grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                UpdatesReceiver().removeUpdates(this)
            } else {
                UpdatesReceiver().setUpdates(this)
            }
            Constants.LOCATION_REQUEST_CODE -> if (permissions.size != 1 || grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                WeatherReceiver().removeUpdates(this)
            } else {
                WeatherReceiver().setUpdates(this)
            }
        }
    }

    fun updateUI() {
        no_calendar_permission_container.visibility= View.GONE
        no_location_permission_container.visibility= View.GONE

        if (!Util.checkGrantedPermission(this, Manifest.permission.READ_CALENDAR)) {
            no_calendar_permission_container.visibility = View.VISIBLE
            request_calendar.setOnClickListener(object: View.OnClickListener {
                override fun onClick(view: View?) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_CALENDAR), Constants.CALENDAR_REQUEST_CODE)
                }
            })
        } else {
            if (!Util.checkGrantedPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                no_location_permission_container.visibility = View.VISIBLE
                request_location.setOnClickListener(object: View.OnClickListener {
                    override fun onClick(view: View?) {
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), Constants.LOCATION_REQUEST_CODE)
                    }
                })
            }
        }
    }


}
