package com.voiddevelopers.wastatussaver.core.analytics

/**
 * Contract every analytics client (Mixpanel, Firebase, ...) must implement.
 */
interface Analytics {

    /** Initializes the underlying SDK. Must be called once before tracking, e.g. in Application.onCreate(). */
    fun init()

    fun track(eventName: String, properties: Map<String, Any?> = emptyMap())

    fun setSuperProperties(properties: Map<String, Any?>)
}
