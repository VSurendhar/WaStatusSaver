package com.voiddevelopers.wastatussaver.presentation.ui.player.ui.imagePlayerRoot

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiddevelopers.wastatussaver.core.analytics.AppAnalytics
import com.voiddevelopers.wastatussaver.core.analytics.DownloadSource
import com.voiddevelopers.wastatussaver.core.analytics.PlayerSource
import com.voiddevelopers.wastatussaver.domain.model.MediaFile
import com.voiddevelopers.wastatussaver.domain.model.MediaInfo
import com.voiddevelopers.wastatussaver.domain.model.MediaType
import com.voiddevelopers.wastatussaver.domain.model.emptyMediaInfo
import com.voiddevelopers.wastatussaver.domain.usecases.SavedMediaHandlingUserCase
import com.voiddevelopers.wastatussaver.presentation.ui.player.ui.PlayerActivity
import com.voiddevelopers.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot.DownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@HiltViewModel
class ImageViewerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val savedMediaHandlingUserCase: SavedMediaHandlingUserCase,
    private val statusMediaDownloadHandler: SavedMediaHandlingUserCase,
    private val appAnalytics: AppAnalytics
) : ViewModel() {

    companion object {
        private const val KEY_MEDIA_INFO = "mediaInfo"
    }

    private val _uiState = MutableStateFlow(ImagePlayerUiState())
    val uiState = _uiState.asStateFlow()

    // Analytics
    private var source = PlayerSource.HOME
    private var mediaViewedLogged = false // viewed_media is logged once per open (not on rotation)

    init {
        restoreMediaInfo()
    }

    private fun restoreMediaInfo() {
        val mediaInfo = savedStateHandle.get<MediaInfo>(KEY_MEDIA_INFO) ?: return
        updateUiState { copy(mediaInfo = mediaInfo) }
    }

    fun handleIntent(intent: Intent) {
        source = PlayerActivity.getSource(intent)
        var mediaInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("mediaInfo", MediaInfo::class.java)
        } else {
            intent.getParcelableExtra("mediaInfo")
        }
        if (mediaInfo == null) return
        val prevMediaInfo = savedStateHandle.get<MediaInfo>(KEY_MEDIA_INFO)
        if (prevMediaInfo != null) {
            mediaInfo = prevMediaInfo
        }
        savedStateHandle[KEY_MEDIA_INFO] = mediaInfo

        updateUiState {
            copy(
                mediaInfo = mediaInfo,
                downloadState = mediaInfo.downloadStatus
            )
        }

        // The screen is on screen. Rotation calls handleIntent again, so log only once.
        if (!mediaViewedLogged) {
            mediaViewedLogged = true
            appAnalytics.trackMediaViewed(MediaType.IMAGE, source)
        }
    }

    // Runs once when the viewer is closed (back), not on rotation or backgrounding.
    override fun onCleared() {
        super.onCleared()
        if (mediaViewedLogged) appAnalytics.trackMediaClosed(MediaType.IMAGE, source)
    }

    fun saveCache(uri: String, mediaType: MediaType, context: Context): Uri? {
        return try {
            val suffix = when (mediaType) {
                MediaType.AUDIO -> ".opus"
                MediaType.IMAGE -> ".jpg"
                MediaType.VIDEO -> ".mp4"
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

    fun getMediaUriFromDownloads(fileName: String, mediaFolder: String): Uri? =
        savedMediaHandlingUserCase.getMediaUriFromDownloads(
            fileName = fileName,
            mediaFolder = mediaFolder
        )

    fun onDownloadClick(mediaFile: MediaFile) {
        viewModelScope.launch {
            updateUiState { copy(downloadState = DownloadState.DOWNLOADING) }
            statusMediaDownloadHandler.saveMediaFile(mediaFile) {
                updateUiState { copy(downloadState = DownloadState.DOWNLOADED) }
                appAnalytics.trackMediaDownloadCompleted(mediaFile.mediaType, DownloadSource.VIEWER)
            }
        }
    }

    fun onShareClick() {
        appAnalytics.trackMediaShareClicked(MediaType.IMAGE, source)
    }

    private fun updateUiState(block: ImagePlayerUiState.() -> ImagePlayerUiState) {
        _uiState.update { it.block() }
    }
}

data class ImagePlayerUiState(
    val mediaInfo: MediaInfo = emptyMediaInfo,
    val showControls: Boolean = true,
    val downloadState: DownloadState = DownloadState.NOT_DOWNLOADED
)
