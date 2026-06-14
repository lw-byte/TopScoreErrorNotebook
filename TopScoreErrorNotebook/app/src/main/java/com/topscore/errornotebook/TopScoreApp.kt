package com.topscore.errornotebook

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TopScoreApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}