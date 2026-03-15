package com.voiddevelopers.wastatussaver.domain.model

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.voiddevelopers.wastatussaver.R
import com.voiddevelopers.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot.DownloadState

@Stable
abstract class MediaFile(initialDownloadState: DownloadState = DownloadState.NOT_DOWNLOADED) {
    val id: String
        get() = fileName
    var downloadState by mutableStateOf(initialDownloadState)
    abstract val mediaType: MediaType
    abstract val fileName: String
    abstract val uri: Uri
    protected val width: Int = 150
    protected val height: Int = 300
}

@Suppress("DEPRECATION")
data class ImageFile(
    override val uri: Uri,
    override val fileName: String,
    override val mediaType: MediaType = MediaType.IMAGE,
    val initialDownloadState: DownloadState = DownloadState.NOT_DOWNLOADED
) : MediaFile(initialDownloadState) {
    override fun toString(): String {
        return "$fileName $downloadState"
    }

}

@Suppress("DEPRECATION")
data class AudioFile(
    override val uri: Uri,
    override val fileName: String,
    override val mediaType: MediaType = MediaType.AUDIO,
    val initialDownloadState: DownloadState = DownloadState.NOT_DOWNLOADED
) : MediaFile(initialDownloadState) {
    private var mediaItem: MediaItem? = null

    fun getMediaItem(context: Context): MediaItem {
        if (mediaItem == null) {
            val artworkUri =
                "android.resource://${context.packageName}/${R.drawable.ic_audio_preview}".toUri()
            mediaItem = MediaItem.Builder().setUri(uri).setMediaMetadata(
                MediaMetadata.Builder().setDisplayTitle(fileName).setArtworkUri(artworkUri).build()
            ).build()
        }
        return mediaItem!!
    }

}


data class VideoFile(
    override val uri: Uri,
    override val fileName: String,
    override val mediaType: MediaType = MediaType.VIDEO,
    val initialDownloadState: DownloadState = DownloadState.NOT_DOWNLOADED
) : MediaFile(initialDownloadState) {
    private var mediaItem: MediaItem = MediaItem.fromUri(uri)

    fun getMediaItem(): MediaItem {
        return mediaItem
    }

}

data class UnknownFile(
    override val uri: Uri,
    override val mediaType: MediaType = MediaType.UNSPECIFIED, 
    override val fileName: String,
    val initialDownloadState: DownloadState = DownloadState.NOT_DOWNLOADED
) : MediaFile(initialDownloadState)
