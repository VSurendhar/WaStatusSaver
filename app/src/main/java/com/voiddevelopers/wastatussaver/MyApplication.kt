package com.voiddevelopers.wastatussaver

import android.app.Application
import com.voiddevelopers.wastatussaver.core.analytics.AppAnalytics
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var appAnalytics: AppAnalytics

    override fun onCreate() {
        super.onCreate()
        appAnalytics.init()
    }

}
