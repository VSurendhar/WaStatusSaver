package com.voiddevelopers.wastatussaver.presentation.ui.webView

import androidx.lifecycle.ViewModel
import com.voiddevelopers.wastatussaver.core.analytics.AppAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Analytics for the Privacy Policy screen.
 * - viewed: the screen was opened
 * - loaded / failed: the result of each page load. WebView can report "finished" twice, or
 *   "finished" after an error, so only the first result of each load is logged.
 */
@HiltViewModel
class WebViewViewModel @Inject constructor(private val appAnalytics: AppAnalytics) : ViewModel() {

    private var alreadyLogged = false

    init {
        appAnalytics.trackPrivacyPolicyViewed()
    }

    override fun onCleared() {
        super.onCleared()
        appAnalytics.trackPrivacyPolicyClosed()
    }

    // Call before every page load.
    fun startNewLoad() {
        alreadyLogged = false
    }

    // Call when the page loaded (true) or failed (false, with the error code and description
    // when there is one). Only the first call per load counts.
    fun logLoadResult(success: Boolean, errorCode: Int? = null, errorDescription: String? = null) {
        if (alreadyLogged) return
        alreadyLogged = true
        if (success) {
            appAnalytics.trackPrivacyPolicyLoaded()
        } else {
            appAnalytics.trackPrivacyPolicyFailed(errorCode, errorDescription)
        }
    }

    fun onRefreshClick() {
        appAnalytics.trackPrivacyPolicyRefreshClicked()
    }
}
