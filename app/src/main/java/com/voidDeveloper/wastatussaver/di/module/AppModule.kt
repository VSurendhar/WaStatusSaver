package com.voidDeveloper.wastatussaver.di.module

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStorePreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Provides
    @Singleton
    fun provideDataStoreManager(
        @ApplicationContext context: Context,
    ): DataStorePreferenceManager {
        return DataStoreManager(context)
    }

    @Provides
    @Singleton
    fun provideWebView(@ApplicationContext context: Context): WebView {
        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            settings.domStorageEnabled = true
            settings.javaScriptEnabled = false
        }
    }

}