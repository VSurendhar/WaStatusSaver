package com.voidDeveloper.wastatussaver.domain.model

sealed class AudioFocusAction {
    object Pause : AudioFocusAction()
    object Play : AudioFocusAction()
    data class SetVolume(val volume: Float) : AudioFocusAction()
}