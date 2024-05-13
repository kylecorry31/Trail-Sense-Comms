package com.kylecorry.trail_sense_comms.app

import android.app.Application
import com.kylecorry.andromeda.preferences.PreferenceMigration
import com.kylecorry.andromeda.preferences.PreferenceMigrator
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createChannels(this)
        migratePreferences()
    }

    private fun migratePreferences(){
        val key = "pref_version"
        val version = 0
        val migrations = listOf<PreferenceMigration>()
        PreferenceMigrator(this, key).migrate(version, migrations)
    }
}