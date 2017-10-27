package com.tommasoberlose.anotherwidget.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.BottomSheetDialog
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.View
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.util.CalendarUtil
import com.tommasoberlose.anotherwidget.util.Util
import com.tommasoberlose.anotherwidget.util.WeatherUtil
import kotlinx.android.synthetic.main.activity_weather_provider.*
import kotlinx.android.synthetic.main.key_time_wait_layout.view.*
import kotlinx.android.synthetic.main.main_menu_layout.view.*
import kotlinx.android.synthetic.main.provider_info_layout.view.*

class WeatherProviderActivity : AppCompatActivity() {

    lateinit var SP: SharedPreferences

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_provider)

        SP = PreferenceManager.getDefaultSharedPreferences(this)

        updateUI()

        action_save.setOnClickListener {
            SP.edit()
                    .putString(Constants.PREF_WEATHER_PROVIDER_API_KEY, api_key.text.toString())
                    .commit()
            setResult(Activity.RESULT_OK)
            finish()
        }

        api_key.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(text: Editable?) {
                if (text.toString().equals("") || text.toString().equals(SP.getString(Constants.PREF_WEATHER_PROVIDER_API_KEY, ""))) {
                    Util.collapse(button_container)
                } else {
                    Util.expand(button_container)
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        action_open_provider.setOnClickListener {
            Util.openURI(this, "https://home.openweathermap.org/users/sign_up")
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun updateUI() {
        val currentProvider = SP.getInt(Constants.PREF_WEATHER_PROVIDER, Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS)
        if (currentProvider == Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS) {
            api_key_container.visibility = View.GONE
            content_info.visibility = View.GONE
        } else {
            api_key_container.visibility = View.VISIBLE
            content_info.visibility = View.VISIBLE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                last_info.text = Html.fromHtml(getString(R.string.api_key_info_all_set), Html.FROM_HTML_MODE_LEGACY)
            } else {
                last_info.text = Html.fromHtml(getString(R.string.api_key_info_all_set))
            }
        }
        label_weather_provider.text = when (currentProvider) {
            Constants.WEATHER_PROVIDER_OPEN_WEATHER -> getString(R.string.provider_open_weather)
            else -> getString(R.string.provider_google_awareness)
        }
        action_change_provider.setOnClickListener {
            SP.edit()
                    .putInt(Constants.PREF_WEATHER_PROVIDER, when (currentProvider) {
                        Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS -> Constants.WEATHER_PROVIDER_OPEN_WEATHER
                        else -> Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS
                    }).commit()
            updateUI()
        }

        Util.collapse(button_container)
        api_key.setText(SP.getString(Constants.PREF_WEATHER_PROVIDER_API_KEY, ""))
    }

    override fun onBackPressed() {
        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        if (!SP.getInt(Constants.PREF_WEATHER_PROVIDER, Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS).equals(Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS) && (api_key.text.toString().equals("") || !api_key.text.toString().equals(SP.getString(Constants.PREF_WEATHER_PROVIDER_API_KEY, "")))) {
            AlertDialog.Builder(this)
                    .setMessage(getString(R.string.error_weather_api_key))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { _,_ ->
                        super.onBackPressed()
                    })
                    .show()
        } else {
            super.onBackPressed()
        }
    }
}
