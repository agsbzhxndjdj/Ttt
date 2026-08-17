package com.example.telecinema

import android.app.Application
import com.example.telecinema.data.local.AppPreferences
import com.example.telecinema.data.download.AppDownloadManager

class TeleCinemaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPreferences.init(this)
        AppDownloadManager.init(this)
    }
}
