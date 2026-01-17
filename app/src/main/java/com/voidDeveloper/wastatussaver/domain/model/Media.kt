package com.voidDeveloper.wastatussaver.domain.model

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot.DownloadState

@Stable
abstract class MediaFile() {
    val id: String
        get() = fileName
    var downloadState by mutableStateOf(DownloadState.NOT_DOWNLOADED)
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
) : MediaFile() {
    override fun toString(): String {
        return "$fileName $downloadState"
    }

}

@Suppress("DEPRECATION")
data class AudioFile(
    override val uri: Uri,
    override val fileName: String,
    override val mediaType: MediaType = MediaType.AUDIO,
) : MediaFile() {
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
) : MediaFile() {
    private var mediaItem: MediaItem = MediaItem.fromUri(uri)

    fun getMediaItem(): MediaItem {
        return mediaItem
    }

}

data class UnknownFile(
    override val uri: Uri,
    override val mediaType: MediaType = MediaType.UNSPECIFIED, override val fileName: String,
) : MediaFile()