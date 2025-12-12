package com.voidDeveloper.wastatussaver

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.voidDeveloper.wastatussaver.data.utils.helpers.WebViewManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application() {
    @Inject
    lateinit var appProcessObserver: AppProcessObserver

    override fun onCreate() {
        super.onCreate()
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