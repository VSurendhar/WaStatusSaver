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
class AppAnalytics @Inject constructor(private val mixpanelAnalytics: MixpanelAnalytics) {

    private val clients: List<Analytics> = listOf(mixpanelAnalytics)

    fun init() {
        clients.forEach { it.init() }
    }

    private fun track(eventName: String, properties: Map<String, Any?> = emptyMap()) {
        clients.forEach { it.track(eventName, properties) }
    }

    private fun setSuperProperties(properties: Map<String, Any?>) {
        clients.forEach { it.setSuperProperties(properties) }
    }

    // ---- Super properties (sent with every event) ----

    /**
     * [selectedTitle]: whatsapp | whatsapp_business.
     * [hasSafPermission] and [isAppInstalled] are for the selected title.
     */
    fun updateSuperProperties(
        selectedTitle: String,
        hasSafPermission: Boolean,
        isAppInstalled: Boolean
    ) {
        setSuperProperties(
            mapOf(
                AnalyticsProperties.SELECTED_TITLE to selectedTitle,
                AnalyticsProperties.HAS_SAF_PERMISSION to hasSafPermission,
                AnalyticsProperties.IS_APP_INSTALLED to isAppInstalled
            )
        )
    }

    // ---- App ----

    fun trackAppStarted() = track(AnalyticsEvents.STARTED_APP)

    fun trackAppClosed() = track(AnalyticsEvents.CLOSED_APP)

    // ---- Navigation drawer ----

    fun trackNavDrawerOpened() = track(AnalyticsEvents.OPENED_NAV_DRAWER)

    fun trackNavDrawerClosed() = track(AnalyticsEvents.CLOSED_NAV_DRAWER)

    /** [option]: how_to_use | saved_status | privacy_policy | rate_app | star_on_github | report_bug | about */
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
                AnalyticsProperties.SOURCE to source.analyticsName
            )
        )
    }

    // ---- Home screen ----

    fun trackTitleChanged() = track(AnalyticsEvents.CHANGED_TITLE)

    fun trackStatusMediaRefreshClicked() = track(AnalyticsEvents.CLICKED_REFRESH_STATUS_MEDIA)

    fun trackDownloadAppClicked() = track(AnalyticsEvents.CLICKED_DOWNLOAD_APP)

    fun trackGrantStorageAccessClicked() = track(AnalyticsEvents.CLICKED_GRANT_STORAGE_ACCESS)

    fun trackStorageAccessGranted() = track(AnalyticsEvents.GRANTED_STORAGE_ACCESS)

    fun trackStorageAccessDenied(reason: StorageAccessDeniedReason) {
        track(
            AnalyticsEvents.DENIED_STORAGE_ACCESS,
            mapOf(AnalyticsProperties.REASON to reason.analyticsName)
        )
    }

    // ---- Downloads ----

    fun trackMediaDownloadCompleted(mediaType: MediaType, source: DownloadSource) {
        track(
            AnalyticsEvents.COMPLETED_MEDIA_DOWNLOAD,
            mapOf(
                AnalyticsProperties.MEDIA_TYPE to mediaType.name.lowercase(),
                AnalyticsProperties.SOURCE to source.analyticsName
            )
        )
    }

    // ---- Onboarding dialog ----

    fun trackOnboardingViewed(source: OnboardingSource) {
        track(
            AnalyticsEvents.VIEWED_ONBOARDING,
            mapOf(AnalyticsProperties.SOURCE to source.analyticsName)
        )
    }

    /** [page]: 1 | 2 | 3. Next on the last page closes the dialog (onboarding completed). */
    fun trackOnboardingNextClicked(page: Int, source: OnboardingSource) {
        track(AnalyticsEvents.CLICKED_ONBOARDING_NEXT, onboardingProperties(page, source))
    }

    /** [page]: 1 | 2 | 3 */
    fun trackOnboardingSkipClicked(page: Int, source: OnboardingSource) {
        track(AnalyticsEvents.CLICKED_ONBOARDING_SKIP, onboardingProperties(page, source))
    }

    private fun onboardingProperties(page: Int, source: OnboardingSource) = mapOf(
        AnalyticsProperties.PAGE to page,
        AnalyticsProperties.SOURCE to source.analyticsName
    )

    // ---- Media player (image viewer, video & audio player) ----

    fun trackMediaViewed(mediaType: MediaType, source: PlayerSource) {
        track(AnalyticsEvents.VIEWED_MEDIA, playerProperties(mediaType, source))
    }

    fun trackMediaClosed(mediaType: MediaType, source: PlayerSource) {
        track(AnalyticsEvents.CLOSED_MEDIA, playerProperties(mediaType, source))
    }

    fun trackMediaShareClicked(mediaType: MediaType, source: PlayerSource) {
        track(AnalyticsEvents.CLICKED_SHARE_MEDIA, playerProperties(mediaType, source))
    }

    private fun playerProperties(mediaType: MediaType, source: PlayerSource) = mapOf(
        AnalyticsProperties.MEDIA_TYPE to mediaType.name.lowercase(),
        AnalyticsProperties.SOURCE to source.analyticsName
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
                AnalyticsProperties.ERROR_DESCRIPTION to errorDescription
            ).filterValues { it != null }
        )
    }

    fun trackPrivacyPolicyRefreshClicked() = track(AnalyticsEvents.CLICKED_REFRESH_PRIVACY_POLICY)
}

enum class DownloadSource(val analyticsName: String) {
    HOME("home"),
    VIEWER("viewer")
}

/** Screen the media player was opened from. */
enum class PlayerSource(val analyticsName: String) {
    HOME("home"),
    SAVED_STATUS("saved_status")
}

/** How the onboarding dialog was opened. */
enum class OnboardingSource(val analyticsName: String) {
    INITIAL_LAUNCH("initial_launch"),
    HOW_TO_USE("how_to_use")
}

enum class StorageAccessDeniedReason(val analyticsName: String) {
    CANCELLED("cancelled"),
    WRONG_FOLDER("wrong_folder")
}

/** Screen a media tab is on. */
enum class MediaTabSource(val analyticsName: String) {
    HOME("home"),
    SAVED_STATUS("saved_status")
}
