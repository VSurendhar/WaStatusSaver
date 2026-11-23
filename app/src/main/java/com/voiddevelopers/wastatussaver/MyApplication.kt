package com.voiddevelopers.wastatussaver

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.voiddevelopers.wastatussaver.core.analytics.AppAnalytics
import com.voiddevelopers.wastatussaver.core.analytics.AppLifecycleTracker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var appAnalytics: AppAnalytics

    @Inject
    lateinit var appLifecycleTracker: AppLifecycleTracker

    override fun onCreate() {
        super.onCreate()
        appAnalytics.init()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleTracker)
    }

}
