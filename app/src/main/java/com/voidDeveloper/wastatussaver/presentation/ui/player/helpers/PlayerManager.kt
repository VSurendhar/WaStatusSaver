package com.voidDeveloper.wastatussaver.presentation.ui.player.helpers

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@ViewModelScoped
class PlayerManager @Inject constructor(
    val player: Player,
    private val audioFocusManager: AudioFocusManager,
) {

    private val _mediaEnded = MutableStateFlow(false)
    val mediaEnded: StateFlow<Boolean> = _mediaEnded

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_ENDED -> _mediaEnded.value = true
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

    }

    init {
        player.addListener(listener)
    }

    fun play() {
        _mediaEnded.value = false
        if (audioFocusManager.requestFocus()) {
            player.play()
        }
    }

    fun pause() {
        player.pause()
        audioFocusManager.abandonFocus()
    }

    fun setVolume(volume: Float) {
        player.volume = volume
    }

    fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean = true) {
        player.setMediaItem(mediaItem, resetPosition)
    }

    fun prepare() {
        player.prepare()
    }

    fun seekTo(positionMs: Long) {
        if (positionMs >= 0) {
            player.seekTo(positionMs)
        }
    }

    fun addListener(listener: Player.Listener) {
        player.addListener(listener)
    }

    fun removeListener(listener: Player.Listener) {
        player.removeListener(listener)
    }

    fun isPlaying(): Boolean = player.isPlaying

    fun currentPosition(): Long = player.currentPosition

    fun duration(): Long = player.duration

    fun release() {
        player.removeListener(listener)
        audioFocusManager.abandonFocus()
        player.release()
    }

}
