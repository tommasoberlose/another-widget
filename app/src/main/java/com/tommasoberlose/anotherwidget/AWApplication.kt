package com.tommasoberlose.anotherwidget

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.chibatching.kotpref.Kotpref
import com.tommasoberlose.anotherwidget.global.Preferences
import io.realm.Realm
import io.realm.RealmConfiguration

class AWApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Preferences
        Kotpref.init(this)

        // Dark theme
        AppCompatDelegate.setDefaultNightMode(Preferences.darkThemePreference)

        // Realm
        Realm.init(this)
        val config = RealmConfiguration.Builder()
            .deleteRealmIfMigrationNeeded()
            .build()
        Realm.setDefaultConfiguration(config)

        calibrateVersions()
    }

    private fun calibrateVersions() {
        // 2.0 Tolerance
        if (Preferences.clockTextSize > 50f) {
            Preferences.clockTextSize = 32f
        }

        if (Preferences.textMainSize > 36f) {
            Preferences.textMainSize = 32f
        }

        if (Preferences.textSecondSize > 28f) {
            Preferences.textSecondSize = 24f
        }
    }
}