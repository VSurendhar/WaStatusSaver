package com.voiddevelopers.wastatussaver.core.analytics

/** Central place for analytics event names and property keys, so they stay consistent. */
object AnalyticsEvents {
    const val OPENED_NAV_DRAWER = "opened_nav_drawer"
    const val CLOSED_NAV_DRAWER = "closed_nav_drawer"

    const val VIEWED_PRIVACY_POLICY = "viewed_privacy_policy"
    const val FAILED_PRIVACY_POLICY_LOAD = "failed_privacy_policy_load"
    const val CLICKED_REFRESH_PRIVACY_POLICY = "clicked_refresh_privacy_policy"

    fun getDrawerOptionEventName(eventName: String) = "clicked_nav_drawer_$eventName"
}

/** Keys for event properties. */
object AnalyticsProperties {
    const val ERROR_CODE = "error_code"
    const val ERROR_DESCRIPTION = "error_description"
}
