package com.tommasoberlose.anotherwidget.ui.activity

import android.app.Activity
import android.location.Address
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.`object`.CustomLocationEvent
import kotlinx.android.synthetic.main.activity_choose_application.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.tommasoberlose.anotherwidget.`object`.ApplicationListEvent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.text.Editable
import android.text.TextWatcher
import android.util.Log


class ChooseApplicationActivity : AppCompatActivity() {
    lateinit var adapter: ArrayAdapter<String>
    val appList = ArrayList<ApplicationInfo>()
    val appListFiltered = ArrayList<ApplicationInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_application)
        val pm = packageManager

        action_default.setOnClickListener {
            selectDefaultApp()
        }

        adapter = ArrayAdapter(this, R.layout.custom_location_item, appList.map { it.name })
        list_view.adapter = adapter

        list_view.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val resultIntent = Intent()
            resultIntent.putExtra(Constants.RESULT_APP_NAME, pm.getApplicationLabel(appListFiltered[position]).toString())
            resultIntent.putExtra(Constants.RESULT_APP_PACKAGE, appListFiltered[position].packageName)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        location.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(text: Editable?) {
                Thread().run {
                    val appsFiltered = appList.filter { pm.getApplicationLabel(it).toString().contains(text.toString(), true) }
                    EventBus.getDefault().post(ApplicationListEvent(appsFiltered, true))
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

    }

    fun selectDefaultApp() {
        val resultIntent = Intent()
        resultIntent.putExtra(Constants.RESULT_APP_NAME, getString(R.string.default_name))
        resultIntent.putExtra(Constants.RESULT_APP_PACKAGE, "")
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    public override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    public override fun onResume() {
        super.onResume()
        Thread().run {
            val pm = packageManager
            val apps = pm.getInstalledApplications(0)
            EventBus.getDefault().post(ApplicationListEvent(apps, false))
        }
    }

    public override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onMessageEvent(event: ApplicationListEvent) {
        val pm = packageManager
        adapter.clear()
        if (!event.filtered) {
            appList.clear()
            event.apps.mapTo(appList, {it})
            for (a:ApplicationInfo in appList) {
                adapter.add(pm.getApplicationLabel(a).toString())
            }
        }
        appListFiltered.clear()
        event.apps.mapTo(appListFiltered, {it})
        for (a:ApplicationInfo in appListFiltered) {
            adapter.add(pm.getApplicationLabel(a).toString())
        }
        adapter.notifyDataSetChanged()
    }
}
