package com.voiddevelopers.wastatussaver.domain.model

import androidx.annotation.Keep

@Keep
sealed class AudioFocusAction {
    @Keep
    object Pause : AudioFocusAction()

    @Keep
    object Play : AudioFocusAction()

    @Keep
    data class SetVolume(val volume: Float) : AudioFocusAction()
}