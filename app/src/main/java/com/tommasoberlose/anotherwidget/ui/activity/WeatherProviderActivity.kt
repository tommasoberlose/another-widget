package com.tommasoberlose.anotherwidget.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.`object`.Constants
import kotlinx.android.synthetic.main.activity_weather_provider.*

class WeatherProviderActivity : AppCompatActivity() {

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_provider)

        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        action_paste.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            api_key.setText(clipboard.primaryClip.getItemAt(0).text)
        }

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
                    action_save.animate().scaleY(-2f).start()
                } else {
                    action_save.animate().scaleY(0f).start()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })
    }

    override fun onBackPressed() {
        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        if (api_key.text.toString().equals("") || !api_key.text.toString().equals(SP.getString(Constants.PREF_WEATHER_PROVIDER_API_KEY, ""))) {
            AlertDialog.Builder(this)
                    .setTitle(getString(R.string.error_weather_api_key))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { _,_ ->
                        super.onBackPressed()
                    })
                    .show()
        } else {
            super.onBackPressed()
        }
    }
}
