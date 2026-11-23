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
    const val VIEWED_MEDIA_TAB_SAVED_STATUS = "viewed_media_tab_saved_status"
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
    const val ERROR_CODE = "error_code"
    const val ERROR_DESCRIPTION = "error_description"
}
