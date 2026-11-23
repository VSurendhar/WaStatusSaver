package com.voiddevelopers.wastatussaver.core.analytics

import com.voiddevelopers.wastatussaver.domain.model.MediaType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for analytics in the app. Fans every call out to all
 * registered analytics clients, so callers trigger an event only once.
 * To add a new client (e.g. Firebase), inject it here and add it to [clients].
 *
 * Callers (ViewModels etc.) call the named event functions below; [track] is private.
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

    private fun track(eventName: String, properties: Map<String, Any?> = emptyMap()) {
        clients.forEach { it.track(eventName, properties) }
    }

    fun setSuperProperties(properties: Map<String, Any?>) {
        clients.forEach { it.setSuperProperties(properties) }
    }

    // ---- App ----

    fun trackAppStarted() = track(AnalyticsEvents.STARTED_APP)

    fun trackAppClosed() = track(AnalyticsEvents.CLOSED_APP)

    // ---- Navigation drawer ----

    fun trackNavDrawerOpened() = track(AnalyticsEvents.OPENED_NAV_DRAWER)

    fun trackNavDrawerClosed() = track(AnalyticsEvents.CLOSED_NAV_DRAWER)

    /** [option]: how_to_use | saved_status | privacy_policy | rate_app | report_bug | about */
    fun trackNavDrawerOptionClicked(option: String) {
        track(
            AnalyticsEvents.CLICKED_NAV_DRAWER_OPTION,
            mapOf(AnalyticsProperties.NAV_DRAWER_OPTION to option)
        )
    }

    // ---- Saved Status ----

    fun trackSavedStatusScreenViewed() = track(AnalyticsEvents.VIEWED_SAVED_STATUS_SCREEN)

    fun trackSavedStatusScreenClosed() = track(AnalyticsEvents.CLOSED_SAVED_STATUS_SCREEN)

    // ---- Media tabs ----

    fun trackMediaTabViewed(mediaType: MediaType, source: MediaTabSource) {
        track(
            AnalyticsEvents.VIEWED_MEDIA_TAB,
            mapOf(
                AnalyticsProperties.MEDIA_TYPE to mediaType.name.lowercase(),
                AnalyticsProperties.SOURCE to source.analyticsName,
            )
        )
    }

    // ---- Downloads ----

    fun trackMediaDownloadCompleted(mediaType: MediaType, source: DownloadSource) {
        track(
            AnalyticsEvents.COMPLETED_MEDIA_DOWNLOAD,
            mapOf(
                AnalyticsProperties.MEDIA_TYPE to mediaType.name.lowercase(),
                AnalyticsProperties.DOWNLOAD_SOURCE to source.analyticsName,
            )
        )
    }

    // ---- Media player (image viewer, video & audio player) ----

    fun trackMediaViewed(mediaType: MediaType, openedFrom: PlayerOpenedFrom) {
        track(AnalyticsEvents.VIEWED_MEDIA, playerProperties(mediaType, openedFrom))
    }

    fun trackMediaClosed(mediaType: MediaType, openedFrom: PlayerOpenedFrom) {
        track(AnalyticsEvents.CLOSED_MEDIA, playerProperties(mediaType, openedFrom))
    }

    fun trackMediaShareClicked(mediaType: MediaType, openedFrom: PlayerOpenedFrom) {
        track(AnalyticsEvents.CLICKED_SHARE_MEDIA, playerProperties(mediaType, openedFrom))
    }

    private fun playerProperties(mediaType: MediaType, openedFrom: PlayerOpenedFrom) = mapOf(
        AnalyticsProperties.MEDIA_TYPE to mediaType.name.lowercase(),
        AnalyticsProperties.OPENED_FROM to openedFrom.analyticsName,
    )

    // ---- Privacy Policy ----

    fun trackPrivacyPolicyViewed() = track(AnalyticsEvents.VIEWED_PRIVACY_POLICY)

    fun trackPrivacyPolicyClosed() = track(AnalyticsEvents.CLOSED_PRIVACY_POLICY)

    fun trackPrivacyPolicyLoaded() = track(AnalyticsEvents.LOADED_PRIVACY_POLICY)

    /** Missing values are left out instead of being sent as null. */
    fun trackPrivacyPolicyFailed(errorCode: Int?, errorDescription: String?) {
        track(
            AnalyticsEvents.FAILED_PRIVACY_POLICY,
            mapOf(
                AnalyticsProperties.ERROR_CODE to errorCode,
                AnalyticsProperties.ERROR_DESCRIPTION to errorDescription,
            ).filterValues { it != null }
        )
    }

    fun trackPrivacyPolicyRefreshClicked() = track(AnalyticsEvents.CLICKED_REFRESH_PRIVACY_POLICY)
}

enum class DownloadSource(val analyticsName: String) {
    HOME("home"),
    VIEWER("viewer"),
}

/** Screen the media player was opened from. */
enum class PlayerOpenedFrom(val analyticsName: String) {
    HOME("home"),
    SAVED_STATUS("saved_status"),
}

/** Screen a media tab is on. */
enum class MediaTabSource(val analyticsName: String) {
    HOME("home"),
    SAVED_STATUS("saved_status"),
}
