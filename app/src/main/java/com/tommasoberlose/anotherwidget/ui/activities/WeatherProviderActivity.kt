package com.tommasoberlose.anotherwidget.ui.activities

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.utils.openURI
import kotlinx.android.synthetic.main.activity_weather_provider.*

class WeatherProviderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_provider)

        action_back.setOnClickListener {
            onBackPressed()
        }

        action_save.setOnClickListener {
            Preferences.weatherProviderApi = api_key.editText?.text.toString()
            setResult(Activity.RESULT_OK)
            finish()
        }

        action_open_provider.setOnClickListener {
            openURI("https://home.openweathermap.org/users/sign_up")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            last_info.text = Html.fromHtml(getString(R.string.api_key_info_all_set), Html.FROM_HTML_MODE_LEGACY)
        } else {
            last_info.text = Html.fromHtml(getString(R.string.api_key_info_all_set))
        }
        api_key.editText?.setText(Preferences.weatherProviderApi)
    }
}
