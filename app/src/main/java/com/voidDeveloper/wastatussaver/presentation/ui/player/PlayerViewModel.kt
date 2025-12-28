package com.voidDeveloper.wastatussaver.presentation.ui.player

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    val player: Player,
) : ViewModel() {

    private val videoUris = savedStateHandle.getStateFlow("videoUri", "".toUri())

    init {
        player.prepare()
    }

    fun addVideoUri(uri: Uri) {
        savedStateHandle["videoUri"] = uri
    }

    fun playVideo() {
        player.setMediaItem(
            MediaItem.fromUri(videoUris.value)
        )
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}