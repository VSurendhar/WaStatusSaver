package com.voidDeveloper.wastatussaver

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.voidDeveloper.wastatussaver.data.utils.helpers.WebViewManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.work.Configuration

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var appProcessObserver: AppProcessObserver

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appProcessObserver)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

class AppProcessObserver @Inject constructor(
    private val webViewManager: WebViewManager,
) : DefaultLifecycleObserver {

    override fun onDestroy(owner: LifecycleOwner) {
        webViewManager.clearWebViewCache()
    }

}