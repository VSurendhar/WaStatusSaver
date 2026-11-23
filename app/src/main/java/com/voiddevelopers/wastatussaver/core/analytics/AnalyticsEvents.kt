package com.voiddevelopers.wastatussaver.core.analytics

/** Central place for analytics event names and property keys, so they stay consistent. */
object AnalyticsEvents {
    const val STARTED_APP = "started_app"
    const val CLOSED_APP = "closed_app"

    const val OPENED_NAV_DRAWER = "opened_nav_drawer"
    const val CLOSED_NAV_DRAWER = "closed_nav_drawer"
    const val CLICKED_NAV_DRAWER_OPTION = "clicked_nav_drawer_option"

    const val VIEWED_SAVED_STATUS_SCREEN = "viewed_saved_status_screen"
    const val CLOSED_SAVED_STATUS_SCREEN = "closed_saved_status_screen"
    const val VIEWED_MEDIA_TAB = "viewed_media_tab"

    const val COMPLETED_MEDIA_DOWNLOAD = "completed_media_download"

    const val CHANGED_TITLE = "changed_title"
    const val CLICKED_REFRESH_STATUS_MEDIA = "clicked_refresh_status_media"
    const val CLICKED_DOWNLOAD_APP = "clicked_download_app"
    const val CLICKED_GRANT_STORAGE_ACCESS = "clicked_grant_storage_access"
    const val GRANTED_STORAGE_ACCESS = "granted_storage_access"
    const val DENIED_STORAGE_ACCESS = "denied_storage_access"

    const val VIEWED_ONBOARDING = "viewed_onboarding_dialog"
    const val CLICKED_ONBOARDING_NEXT = "clicked_onboarding_next"
    const val CLICKED_ONBOARDING_SKIP = "clicked_onboarding_skip"

    const val VIEWED_MEDIA = "viewed_media"
    const val CLOSED_MEDIA = "closed_media"
    const val CLICKED_SHARE_MEDIA = "clicked_share_media"

    const val VIEWED_PRIVACY_POLICY = "viewed_privacy_policy"
    const val CLOSED_PRIVACY_POLICY = "closed_privacy_policy"
    const val LOADED_PRIVACY_POLICY = "loaded_privacy_policy"
    const val FAILED_PRIVACY_POLICY = "failed_privacy_policy"
    const val CLICKED_REFRESH_PRIVACY_POLICY = "clicked_refresh_privacy_policy"
}

/** Keys for event properties. */
object AnalyticsProperties {
    const val NAV_DRAWER_OPTION = "option"
    const val MEDIA_TYPE = "media_type"
    const val SOURCE = "source"
    const val PAGE = "page"
    const val REASON = "reason"

    // Super properties (sent with every event)
    const val SELECTED_TITLE = "selected_title"
    const val HAS_SAF_PERMISSION = "has_saf_permission"
    const val IS_APP_INSTALLED = "is_app_installed"
    const val ERROR_CODE = "error_code"
    const val ERROR_DESCRIPTION = "error_description"
}
