package com.tommasoberlose.anotherwidget.ui.activities

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.ui.activities.tabs.WeatherProviderActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var mAppWidgetId: Int = -1
    private lateinit var viewModel: MainViewModel
    private val mainNavController: NavController? by lazy {
        Navigation.findNavController(
            this,
            R.id.content_fragment
        )
    }
    private val settingsNavController: NavController? by lazy {
        Navigation.findNavController(
            this,
            R.id.settings_fragment
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        controlExtras(intent)
        if (Preferences.showWallpaper) {
            requirePermission()
        }
    }

    override fun onBackPressed() {
        if (mainNavController?.currentDestination?.id == R.id.appMainFragment) {
            if (settingsNavController?.navigateUp() == false) {
                if (mAppWidgetId > 0) {
                    addNewWidget()
                } else {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            } else {
                viewModel.fragmentScrollY.value = 0
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null) {
            controlExtras(intent)
        }
    }

    private fun controlExtras(intent: Intent) {
        val extras = intent.extras
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID)

            if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                action_add_widget.visibility = View.VISIBLE
                action_add_widget.setOnClickListener {
                    addNewWidget()
                }
            }


            if (extras.containsKey(Actions.ACTION_EXTRA_OPEN_WEATHER_PROVIDER)) {
                startActivityForResult(Intent(this, WeatherProviderActivity::class.java), RequestCode.WEATHER_PROVIDER_REQUEST_CODE.code)
            }
        }
    }

    private fun addNewWidget() {
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }

    private fun requirePermission() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        Preferences.showWallpaper = false
                        Preferences.showWallpaper = report.areAllPermissionsGranted()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    // Remember to invoke this method when the custom rationale is closed
                    // or just by default if you don't want to use any custom rationale.
                    token?.cancelPermissionRequest()
                }
            })
            .check()
    }
}
