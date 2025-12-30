package com.voidDeveloper.wastatussaver.domain.model

import android.os.Parcelable
import androidx.core.net.toUri
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.FileType
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.DownloadState
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaInfo(
    val uri: String,
    val lastPlayedMillis: Long,
    val fileName: String,
    val fileType: FileType,
    val downloadStatus: DownloadState,
) : Parcelable

fun MediaInfo.toMediaFile(): MediaFile {
    return when (fileType) {
        FileType.AUDIO -> {
            AudioFile(uri.toUri(), fileName)
        }

        FileType.IMAGES -> {
            ImageFile(uri.toUri(), fileName)
        }

        FileType.VIDEOS -> {
            VideoFile(uri.toUri(), fileName)
        }

        FileType.UNSPECIFIED -> {
            UnknownFile(uri.toUri())
        }
    }
}

val emptyMediaInfo = MediaInfo(
    "", 0, "", downloadStatus = DownloadState.NOT_DOWNLOADED,
    fileType = FileType.UNSPECIFIED
)