package com.voiddevelopers.wastatussaver.core.analytics

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for analytics in the app. Fans every call out to all
 * registered analytics clients, so callers trigger an event only once.
 * To add a new client (e.g. Firebase), inject it here and add it to [clients].
 */
@Singleton
class AppAnalytics @Inject constructor(
    private val mixpanelAnalytics: MixpanelAnalytics,
) {

    private val clients: List<Analytics> = listOf(mixpanelAnalytics)

    fun init() {
        clients.forEach { it.init() }
    }

    fun track(eventName: String, properties: Map<String, Any?> = emptyMap()) {
        clients.forEach { it.track(eventName, properties) }
    }

    fun setSuperProperties(properties: Map<String, Any?>) {
        clients.forEach { it.setSuperProperties(properties) }
    }
}
