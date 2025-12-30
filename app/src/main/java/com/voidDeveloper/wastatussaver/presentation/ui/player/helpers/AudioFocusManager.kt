package com.voidDeveloper.wastatussaver.presentation.ui.player.helpers

import android.app.Application
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import com.voidDeveloper.wastatussaver.domain.model.AudioFocusAction
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class AudioFocusManager @Inject constructor(
    appContext: Application,
) {

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusListener: ((AudioFocusAction) -> Unit)? = null
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, -> {
                focusListener?.invoke(AudioFocusAction.Pause)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                focusListener?.invoke(AudioFocusAction.SetVolume(0.2f))
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                focusListener?.invoke(AudioFocusAction.SetVolume(1f))
            }
        }
    }

    private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setOnAudioFocusChangeListener(focusChangeListener).setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE).build()
        ).build()

    fun requestFocus(): Boolean {
        val result = audioManager.requestAudioFocus(audioFocusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandonFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    fun setFocusListener(listener: (AudioFocusAction) -> Unit) {
        focusListener = listener
    }

}
