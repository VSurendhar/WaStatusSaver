package com.voidDeveloper.wastatussaver.domain.model

import android.os.Parcelable
import androidx.core.net.toUri
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.DownloadState
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaInfo(
    val uri: String,
    val lastPlayedMillis: Long,
    val fileName: String,
    val mediaType: MediaType,
    var downloadStatus: DownloadState,
) : Parcelable

fun MediaInfo.toMediaFile(): MediaFile {
    return when (mediaType) {
        MediaType.AUDIO -> {
            AudioFile(uri.toUri(), fileName)
        }

        MediaType.IMAGE -> {
            ImageFile(uri.toUri(), fileName)
        }

        MediaType.VIDEO -> {
            VideoFile(uri.toUri(), fileName)
        }

        MediaType.UNSPECIFIED, MediaType.UNRECOGNIZED -> {
            UnknownFile(uri.toUri(), fileName = fileName)
        }
    }
}

val emptyMediaInfo = MediaInfo(
    "", 0, "", downloadStatus = DownloadState.NOT_DOWNLOADED,
    mediaType = MediaType.UNSPECIFIED
)