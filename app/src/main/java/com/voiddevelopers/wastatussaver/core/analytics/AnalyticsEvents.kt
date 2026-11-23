package com.voiddevelopers.wastatussaver.core.analytics

/** Central place for analytics event names and property keys, so they stay consistent. */
object AnalyticsEvents {
    const val OPENED_NAV_DRAWER = "opened_nav_drawer"
    const val CLOSED_NAV_DRAWER = "closed_nav_drawer"

    fun getDrawerOptionEventName(eventName: String) = "clicked_nav_drawer_$eventName"
}
