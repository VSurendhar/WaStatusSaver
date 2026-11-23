package com.voiddevelopers.wastatussaver.core.analytics

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject

class MixpanelAnalytics(
    private val context: Context,
    private val token: String,
) : Analytics {

    private var mixpanel: MixpanelAPI? = null

    override fun init() {
        if (mixpanel != null) return
        mixpanel = MixpanelAPI.getInstance(context, token, false)
    }

    override fun track(eventName: String, properties: Map<String, Any?>) {
        mixpanel?.track(eventName, properties.toJson())
    }

    override fun setSuperProperties(properties: Map<String, Any?>) {
        mixpanel?.registerSuperProperties(properties.toJson())
    }

    /**
     * Mixpanel's SDK-generated device UUID (its anonymous ID). Stays the same across app restarts
     * and updates; changes on reinstall, clear data, or reset(). Null until [init] has been called.
     */
    fun getDeviceId(): String? = mixpanel?.anonymousId

    private fun Map<String, Any?>.toJson(): JSONObject = JSONObject().also { json ->
        forEach { (key, value) -> json.put(key, value ?: JSONObject.NULL) }
    }
}
