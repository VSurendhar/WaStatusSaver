package com.voiddevelopers.wastatussaver.presentation.ui.webView

import androidx.lifecycle.ViewModel
import com.voiddevelopers.wastatussaver.core.analytics.AnalyticsEvents
import com.voiddevelopers.wastatussaver.core.analytics.AnalyticsProperties
import com.voiddevelopers.wastatussaver.core.analytics.AppAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Analytics for the Privacy Policy screen: every page load logs one result, viewed or failed.
 * WebView can report "finished" twice, or "finished" after an error, so only the first result
 * of each load is logged.
 */
@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val appAnalytics: AppAnalytics,
) : ViewModel() {

    private var alreadyLogged = false

    // Call before every page load.
    fun startNewLoad() {
        alreadyLogged = false
    }

    // Call when the page loaded (true) or failed (false, with the error code and description
    // when there is one). Only the first call per load counts.
    fun logLoadResult(
        success: Boolean,
        errorCode: Int? = null,
        errorDescription: String? = null,
    ) {
        if (alreadyLogged) return
        alreadyLogged = true
        if (success) {
            appAnalytics.track(AnalyticsEvents.VIEWED_PRIVACY_POLICY)
        } else {
            val errorProperties = mapOf(
                AnalyticsProperties.ERROR_CODE to errorCode,
                AnalyticsProperties.ERROR_DESCRIPTION to errorDescription,
            ).filterValues { it != null }
            appAnalytics.track(AnalyticsEvents.FAILED_PRIVACY_POLICY_LOAD, errorProperties)
        }
    }

    fun onRefreshClick() {
        appAnalytics.track(AnalyticsEvents.CLICKED_REFRESH_PRIVACY_POLICY)
    }
}
