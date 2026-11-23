package com.voiddevelopers.wastatussaver

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.voiddevelopers.wastatussaver.core.analytics.AppAnalytics
import com.voiddevelopers.wastatussaver.data.utils.helpers.WebViewManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var appProcessObserver: AppProcessObserver

    @Inject
    lateinit var appAnalytics: AppAnalytics

    override fun onCreate() {
        super.onCreate()
        appAnalytics.init()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appProcessObserver)
    }

}

class AppProcessObserver @Inject constructor(
    private val webViewManager: WebViewManager,
) : DefaultLifecycleObserver {

    override fun onDestroy(owner: LifecycleOwner) {
        webViewManager.clearWebViewCache()
    }

}