package com.voidDeveloper.wastatussaver.presentation.ui.player

import android.content.Intent
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.voidDeveloper.wastatussaver.domain.model.MediaInfo
import com.voidDeveloper.wastatussaver.domain.model.emptyMediaInfo
import com.voidDeveloper.wastatussaver.presentation.ui.player.helpers.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
//    val player: Player,
    val playerController: PlayerController,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val KEY_MEDIA_INFO = "mediaInfo"
    }

    val player: Player
        get() = playerController.player

    val mediaInfo: StateFlow<MediaInfo> =
        savedStateHandle
            .getStateFlow(KEY_MEDIA_INFO, emptyMediaInfo)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyMediaInfo
            )

    fun playVideo() {
        Log.i("Surendhar TAG", "playVideo: Playing Video")
        val info = mediaInfo.value
        playerController.player.setMediaItem(MediaItem.fromUri(info.uri))
        playerController.player.prepare()
        val lastPlayedMillis = info.lastPlayedMillis
        if (lastPlayedMillis != 0L && lastPlayedMillis < playerController.player.duration) {
            playerController.player.seekTo(lastPlayedMillis)
        }
        playerController.player.play()
    }

    override fun onCleared() {
        super.onCleared()
        playerController.player.release()
    }

    fun addVideoUri(uriString: String, fileName: String) {
        val newMediaInfo = MediaInfo(
            uri = uriString,
            fileName = fileName,
            lastPlayedMillis = 0L
        )
        savedStateHandle[KEY_MEDIA_INFO] = newMediaInfo
    }

    fun handleIntent(intent: Intent) {
        val newMediaInfo = MediaInfo(
            uri = intent.getStringExtra("videoUri") ?: return,
            fileName = intent.getStringExtra("fileName") ?: "",
            lastPlayedMillis = 0L
        )
        savedStateHandle[KEY_MEDIA_INFO] = newMediaInfo
    }

    fun persistPlaybackPosition() {
        val current = savedStateHandle.get<MediaInfo>(KEY_MEDIA_INFO) ?: return
        savedStateHandle[KEY_MEDIA_INFO] =
            current.copy(lastPlayedMillis = playerController.player.currentPosition)
    }

}