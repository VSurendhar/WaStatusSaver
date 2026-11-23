package com.voiddevelopers.wastatussaver.core.analytics

/** Central place for analytics event names and property keys, so they stay consistent. */
object AnalyticsEvents {
    const val OPENED_NAV_DRAWER = "opened_nav_drawer"
    const val CLOSED_NAV_DRAWER = "closed_nav_drawer"
    const val CLICKED_NAV_DRAWER_OPTION = "clicked_nav_drawer_option"

    const val VIEWED_PRIVACY_POLICY = "viewed_privacy_policy"
    const val CLOSED_PRIVACY_POLICY = "closed_privacy_policy"
    const val LOADED_PRIVACY_POLICY = "loaded_privacy_policy"
    const val FAILED_PRIVACY_POLICY = "failed_privacy_policy"
    const val CLICKED_REFRESH_PRIVACY_POLICY = "clicked_refresh_privacy_policy"
}

/** Keys for event properties. */
object AnalyticsProperties {
    const val NAV_DRAWER_OPTION = "option"
    const val ERROR_CODE = "error_code"
    const val ERROR_DESCRIPTION = "error_description"
}
