package com.tommasoberlose.anotherwidget.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
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
import kotlinx.android.synthetic.main.main_menu_layout.view.*
import kotlinx.android.synthetic.main.provider_info_layout.view.*

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

        Util.collapse(button_container)
        api_key.setText(SP.getString(Constants.PREF_WEATHER_PROVIDER_API_KEY, ""))

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

        action_open_info_text.setOnClickListener {
            val mBottomSheetDialog: BottomSheetDialog = BottomSheetDialog(this)
            val provView: View = layoutInflater.inflate(R.layout.provider_info_layout, null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                provView.text.text = Html.fromHtml(getString(R.string.api_key_info_text), Html.FROM_HTML_MODE_LEGACY)
            } else {
                provView.text.text = Html.fromHtml(getString(R.string.api_key_info_text))
            }
            mBottomSheetDialog.setContentView(provView)
            mBottomSheetDialog.show();
        }
    }

    override fun onBackPressed() {
        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        if (api_key.text.toString().equals("") || !api_key.text.toString().equals(SP.getString(Constants.PREF_WEATHER_PROVIDER_API_KEY, ""))) {
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
