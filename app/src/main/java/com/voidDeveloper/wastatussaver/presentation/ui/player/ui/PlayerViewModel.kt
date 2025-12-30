package com.voidDeveloper.wastatussaver.presentation.ui.player.ui

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.voidDeveloper.wastatussaver.domain.model.MediaInfo
import com.voidDeveloper.wastatussaver.domain.model.emptyMediaInfo
import com.voidDeveloper.wastatussaver.domain.model.AudioFocusAction
import com.voidDeveloper.wastatussaver.domain.model.MediaFile
import com.voidDeveloper.wastatussaver.domain.usecases.StatusesManagerUseCase
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.FileType
import com.voidDeveloper.wastatussaver.presentation.ui.player.helpers.AudioFocusManager
import com.voidDeveloper.wastatussaver.presentation.ui.player.helpers.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val audioFocusManager: AudioFocusManager,
    private val savedStateHandle: SavedStateHandle,
    private val statusesManagerUseCase: StatusesManagerUseCase,
) : ViewModel() {

    companion object {
        private const val KEY_MEDIA_INFO = "mediaInfo"
    }

    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    private val _onRestore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val onRestore = _onRestore.asStateFlow()

    private val _isDownloadStatus: MutableStateFlow<DownloadState> =
        MutableStateFlow(DownloadState.NOT_DOWNLOADED)
    val isDownloadStatus = _isDownloadStatus.asStateFlow()

    init {
        audioFocusManager.setFocusListener { action ->
            when (action) {

                AudioFocusAction.Pause -> {
                    pause()
                }

                is AudioFocusAction.SetVolume -> {
                    setVolume(action.volume)
                }

                else -> {}
            }
        }
    }

    val player: Player
        get() = playerManager.player

    val mediaInfo: StateFlow<MediaInfo> =
        savedStateHandle.getStateFlow(KEY_MEDIA_INFO, emptyMediaInfo).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMediaInfo
        )

    val mediaEnded = playerManager.mediaEnded

    fun setMediaItem() {
        val info = mediaInfo.value

        val listener = object : Player.Listener {

            private var restored = false

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && !restored) {
                    restored = true

                    if (info.lastPlayedMillis > 0) {
                        playerManager.seekTo(info.lastPlayedMillis)
                    }

                    _onRestore.value = true

                    if (!playerManager.isPlaying.value) {
                        play()
                    }

                    playerManager.removeListener(this)
                }
            }
        }

        playerManager.addListener(listener)
        playerManager.setMediaItem(MediaItem.fromUri(info.uri))
        playerManager.prepare()
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }


    fun play() {
        playerManager.play()
    }

    fun pause() {
        playerManager.pause()
    }


    fun setVolume(volume: Float) {
        playerManager.setVolume(volume)
    }

    fun handleIntent(intent: Intent) {
        val previous = savedStateHandle.get<Any>(KEY_MEDIA_INFO)

        val lastPlayedMillis = (previous as? MediaInfo)?.lastPlayedMillis ?: 0L

        val mediaInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("mediaInfo", MediaInfo::class.java)
        } else {
            intent.getParcelableExtra("mediaInfo")
        }

        _isDownloadStatus.value = mediaInfo?.downloadStatus ?: DownloadState.NOT_DOWNLOADED

        val newMediaInfo = mediaInfo?.copy(
            lastPlayedMillis = lastPlayedMillis
        )

        if (newMediaInfo != null) {
            savedStateHandle[KEY_MEDIA_INFO] = newMediaInfo
        }

    }

    fun onDownloadClick(mediaFile: MediaFile) {
        viewModelScope.launch {
            _isDownloadStatus.value = DownloadState.DOWNLOADING
            statusesManagerUseCase.saveMediaFile(mediaFile, onSaveCompleted = {
                _isDownloadStatus.value = DownloadState.DOWNLOADED
            })
        }
    }

    fun persistPlaybackPosition() {
        val current = savedStateHandle.get<MediaInfo>(KEY_MEDIA_INFO) ?: return
        savedStateHandle[KEY_MEDIA_INFO] =
            current.copy(lastPlayedMillis = playerManager.currentPosition())
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

    fun onRestored() {
        _onRestore.value = false
    }

    fun saveCache(
        uri: String,
        fileType: FileType,
        context: Context,
    ): Uri? {
        return try {

            val suffix = when (fileType) {
                FileType.AUDIO -> ".opus"
                FileType.IMAGES -> ".jpg"
                FileType.VIDEOS -> ".mp4"
                else -> ""
            }

            val file = File.createTempFile(
                "file_${System.currentTimeMillis()}",
                suffix,
                context.cacheDir
            )

            context.contentResolver.openInputStream(uri.toUri())?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun getMediaUriFromDownloads(
        context: Context,
        fileName: String,
        mediaFolder: String,
    ): Uri? {

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME
        )

        val selection: String?
        val selectionArgs: Array<String>?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection =
                "${MediaStore.Downloads.DISPLAY_NAME} = ? AND " +
                        "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"

            selectionArgs = arrayOf(
                fileName,
                "%Download/WaStatusSaver/$mediaFolder/%"
            )
        } else {
            selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            selectionArgs = arrayOf(fileName)
        }

        return try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {
                    val idIndex =
                        cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)

                    val id = cursor.getLong(idIndex)

                    ContentUris.withAppendedId(collection, id)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("WaStatusSaver", "Failed to get media URI", e)
            null
        }
    }

}

enum class DownloadState {
    DOWNLOADED, DOWNLOADING, NOT_DOWNLOADED
}