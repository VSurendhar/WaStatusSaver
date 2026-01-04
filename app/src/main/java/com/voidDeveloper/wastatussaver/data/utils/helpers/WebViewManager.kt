package com.voidDeveloper.wastatussaver.data.utils.helpers

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class WebViewManager @Inject constructor(
    private val webView: WebView,
) {
    fun clearWebViewCache() {
        webView.clearCache(true)
        webView.clearHistory()
        WebStorage.getInstance().deleteAllData()
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies { success ->
        }
        cookieManager.flush()
        webView.destroy()
    }

    fun getWebViewInstance(): WebView = webView
}