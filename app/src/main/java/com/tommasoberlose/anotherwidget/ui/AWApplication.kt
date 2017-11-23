package com.tommasoberlose.anotherwidget.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.util.MyMigration
import com.tommasoberlose.anotherwidget.util.Util

import io.realm.Realm
import io.realm.RealmConfiguration

class AWApplication : Application() {
    @SuppressLint("ApplySharedPref")
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        val config = RealmConfiguration.Builder()
                .schemaVersion(2)
                .migration(MyMigration())
                .deleteRealmIfMigrationNeeded()
                .build()
        Realm.setDefaultConfiguration(config)
    }
}