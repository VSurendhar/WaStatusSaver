package com.voiddevelopers.wastatussaver.core.analytics

import com.voiddevelopers.wastatussaver.domain.model.MediaType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for analytics in the app. Fans every call out to all
 * registered analytics clients, so callers trigger an event only once.
 * To add a new client (e.g. Firebase), inject it here and add it to [clients].
 *
 * Callers (ViewModels etc.) should call the named event functions below, not [track] directly.
 * Each named function builds its event's properties and calls [track].
 */
@Singleton
class AppAnalytics @Inject constructor(
    private val mixpanelAnalytics: MixpanelAnalytics,
) {

    private val clients: List<Analytics> = listOf(mixpanelAnalytics)

    fun init() {
        clients.forEach { it.init() }
    }

    // TODO: make private once every event has a named function below.
    fun track(eventName: String, properties: Map<String, Any?> = emptyMap()) {
        clients.forEach { it.track(eventName, properties) }
    }

    fun setSuperProperties(properties: Map<String, Any?>) {
        clients.forEach { it.setSuperProperties(properties) }
    }

    // ---- Events ----

    fun trackMediaDownloadCompleted(mediaType: MediaType, source: DownloadSource) {
        track(
            AnalyticsEvents.COMPLETED_MEDIA_DOWNLOAD,
            mapOf(
                AnalyticsProperties.MEDIA_TYPE to mediaType.name.lowercase(),
                AnalyticsProperties.DOWNLOAD_SOURCE to source.analyticsName,
            )
        )
    }
}

enum class DownloadSource(val analyticsName: String) {
    HOME("home"),
    VIEWER("viewer"),
}
