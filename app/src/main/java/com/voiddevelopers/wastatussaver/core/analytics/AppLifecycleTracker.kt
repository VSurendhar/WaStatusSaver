package com.voiddevelopers.wastatussaver.core.analytics

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject

/**
 * Logs app start / close. Registered on ProcessLifecycleOwner, which tracks the whole app:
 * - onStart: the app came to the foreground (fresh launch, or back from the background)
 * - onStop:  the app went to the background (Home button, switching apps)
 * Moving between the app's own screens, or rotating, doesn't trigger either.
 */
class AppLifecycleTracker @Inject constructor(
    private val appAnalytics: AppAnalytics,
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        appAnalytics.track(AnalyticsEvents.STARTED_APP)
    }

    override fun onStop(owner: LifecycleOwner) {
        appAnalytics.track(AnalyticsEvents.CLOSED_APP)
    }
}
