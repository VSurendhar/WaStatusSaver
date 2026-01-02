package com.voidDeveloper.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.domain.model.AudioFocusAction
import com.voidDeveloper.wastatussaver.domain.model.MediaFile
import com.voidDeveloper.wastatussaver.domain.model.MediaInfo
import com.voidDeveloper.wastatussaver.domain.model.emptyMediaInfo
import com.voidDeveloper.wastatussaver.domain.usecases.SavedMediaHandlingUserCase
import com.voidDeveloper.wastatussaver.domain.usecases.StatusesManagerUseCase
import com.voidDeveloper.wastatussaver.presentation.ui.player.helpers.AudioFocusManager
import com.voidDeveloper.wastatussaver.presentation.ui.player.helpers.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AudioVideoPlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val audioFocusManager: AudioFocusManager,
    private val savedStateHandle: SavedStateHandle,
    private val savedMediaHandlingUserCase: SavedMediaHandlingUserCase,
    private val statusesManagerUseCase: StatusesManagerUseCase,
) : ViewModel() {

    companion object {
        private const val KEY_MEDIA_INFO = "mediaInfo"
    }

    private val _uiState = MutableStateFlow(AudioVideoPlayerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeAudioFocus()
        observePlayer()
        restoreMediaInfo()
    }

    private fun observePlayer() {
        playerManager.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateUiState {
                    copy(isPlaying = isPlaying)
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    updateUiState {
                        copy(mediaEnded = true)
                    }
                }
            }
        })
    }

    private fun restoreMediaInfo() {
        val mediaInfo = savedStateHandle.get<MediaInfo>(KEY_MEDIA_INFO) ?: return
        updateUiState { copy(mediaInfo = mediaInfo) }
    }

    private fun observeAudioFocus() {
        audioFocusManager.setFocusListener { action ->
            when (action) {
                AudioFocusAction.Pause -> pause()
                is AudioFocusAction.SetVolume -> setVolume(action.volume)
                else -> Unit
            }
        }
    }


    val player: Player
        get() = playerManager.player


    fun setMediaItem() {
        val info = uiState.value.mediaInfo
        if (info.uri.isEmpty()) return

        var restored = false

        playerManager.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && !restored) {
                    restored = true

                    if (info.lastPlayedMillis > 0) {
                        playerManager.seekTo(info.lastPlayedMillis)
                    }

//                    if (!playerManager.player.isPlaying) {
                    play()
//                    }

                    playerManager.removeListener(this)
                }
            }
        })

        playerManager.setMediaItem(MediaItem.fromUri(info.uri))
        playerManager.prepare()
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }


    fun play() = playerManager.play()

    fun pause() = playerManager.pause()

    fun seekToPercent(percent: Long) {
        val duration = playerManager.duration()
        if (duration > 0) {
            playerManager.seekTo(duration * percent / 100)
        }
    }

    fun persistPlaybackPosition() {
        val current = uiState.value.mediaInfo
        savedStateHandle[KEY_MEDIA_INFO] =
            current.copy(lastPlayedMillis = playerManager.currentPosition())
    }

    fun setVolume(volume: Float) {
        playerManager.setVolume(volume)
    }

    fun handleIntent(intent: Intent) {

        val mediaInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("mediaInfo", MediaInfo::class.java)
        } else {
            intent.getParcelableExtra("mediaInfo")
        }

        if (mediaInfo == null) return

        val lastPlayedMillis =
            savedStateHandle.get<MediaInfo>(KEY_MEDIA_INFO)?.lastPlayedMillis ?: 0L

        val updated = mediaInfo.copy(lastPlayedMillis = lastPlayedMillis)
        savedStateHandle[KEY_MEDIA_INFO] = updated

        updateUiState {
            copy(
                mediaInfo = updated, downloadState = updated.downloadStatus
            )
        }
    }

    fun playerSeekTo(millis: Long) {
        playerManager.seekTo(millis)
    }

    fun getPlayerDuration(): Long {
        return playerManager.duration()
    }

    fun getPlayerCurrentPosition(): Long {
        return playerManager.currentPosition()
    }

    fun isPlayerPlaying(): Boolean {
        return playerManager.isPlaying()
    }


    fun saveCache(
        uri: String,
        mediaType: MediaType,
        context: Context,
    ): Uri? {
        return try {

            val suffix = when (mediaType) {
                MediaType.AUDIO -> ".opus"
                MediaType.IMAGE -> ".jpg"
                MediaType.VIDEO -> ".mp4"
                else -> ""
            }

            val file = File.createTempFile(
                "file_${System.currentTimeMillis()}", suffix, context.cacheDir
            )

            context.contentResolver.openInputStream(uri.toUri())?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun getMediaUriFromDownloads(
        fileName: String,
        mediaFolder: String,
    ): Uri? {
        return savedMediaHandlingUserCase.getMediaUriFromDownloads(
            fileName = fileName, mediaFolder = mediaFolder
        )
    }

    fun updateProgress() {
        val duration = player.duration.coerceAtLeast(0L)
        val position = player.currentPosition

        val percent = if (duration > 0) {
            (position * 100 / duration)
        } else 0L

        updateUiState {
            copy(
                progressPercent = percent, durationMs = duration
            )
        }
    }

    fun toggleControls() {
        updateUiState {
            copy(showControls = !showControls)
        }
    }

    fun onDownloadClick(mediaFile: MediaFile) {
        viewModelScope.launch {
            updateUiState { copy(downloadState = DownloadState.DOWNLOADING) }

            statusesManagerUseCase.saveMediaFile(mediaFile) {
                updateUiState { copy(downloadState = DownloadState.DOWNLOADED) }
            }
        }
    }

    fun onPlaybackStateChanged(
        isPlaying: Boolean,
        ended: Boolean = false,
    ) {
        updateUiState {
            copy(
                isPlaying = isPlaying, mediaEnded = ended
            )
        }
    }

    private fun updateUiState(block: AudioVideoPlayerUiState.() -> AudioVideoPlayerUiState) {
        _uiState.update { it.block() }
    }

}

enum class DownloadState {
    DOWNLOADED, DOWNLOADING, NOT_DOWNLOADED
}

data class AudioVideoPlayerUiState(
    val mediaInfo: MediaInfo = emptyMediaInfo,
    var isPlaying: Boolean = false,
    val mediaEnded: Boolean = false,
    val progressPercent: Long = 0L,
    val durationMs: Long = 0L,
    val showControls: Boolean = true,
    val downloadState: DownloadState = DownloadState.NOT_DOWNLOADED,
)