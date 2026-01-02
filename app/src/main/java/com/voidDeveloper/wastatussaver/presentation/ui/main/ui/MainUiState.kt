package com.voidDeveloper.wastatussaver.presentation.ui.main.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.data.utils.Constants.DEFAULT_AUTO_SAVE_INTERVAL
import com.voidDeveloper.wastatussaver.domain.model.MediaFile
import com.voidDeveloper.wastatussaver.domain.model.MediaInfo
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot.DownloadState

data class UiState(
    val title: Title? = null,
    val appInstalled: Boolean? = null,
    val selectionMode: SelectionMode? = null,
    val currentMediaType: MediaType? = null,
    val shouldShowOnBoardingUi: Boolean? = null,
    val hasSafAccessPermission: Boolean? = null,
    val mediaFiles: List<MediaFile> = emptyList(),
    val onGoingDownload: MutableList<MediaFile> = mutableListOf(),
    val showAutoSaveDialog: Boolean = false,
    val showNotificationPermissionDialog: Boolean = false,
    val showNotificationPermissionSettingsDialog: Boolean = false,
    val savedAutoSaveInterval: Int = DEFAULT_AUTO_SAVE_INTERVAL,
    val autoSaveEnabled: Boolean = false,
    val lastRefreshTimestamp: Long = 0L,
)


fun MediaType.toFileType() = when (this) {
    MediaType.AUDIO -> MediaType.AUDIO
    MediaType.IMAGE -> MediaType.IMAGE
    MediaType.VIDEO -> MediaType.VIDEO
    else -> MediaType.UNSPECIFIED
}

fun MediaFile.toDomainMediaInfo(): MediaInfo {
    return MediaInfo(
        uri = uri.toString(),
        lastPlayedMillis = 0,
        fileName = fileName.toString(),
        mediaType = mediaType,
        downloadStatus = if (isDownloaded) DownloadState.DOWNLOADED else DownloadState.NOT_DOWNLOADED
    )
}

enum class SelectionMode {
    MULTI_SELECT, SINGLE_SELECT
}

sealed class Title(@StringRes val resId: Int, val packageName: String, val uri: Uri) {
    object Whatsapp : Title(
        resId = R.string.title_whatsapp,
        packageName = "com.whatsapp",
        uri = "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses".toUri()
    ) {
        override fun toString(): String {
            return "Whatsapp(resId=$resId, packageName=$packageName,)"
        }
    }

    object WhatsappBusiness : Title(
        resId = R.string.title_whatsapp_business,
        packageName = "com.whatsapp.w4b",
        uri = "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp%20Business%2FMedia%2F.Statuses".toUri()
    ) {
        override fun toString(): String {
            return "WhatsappBusiness(resId=$resId, packageName=$packageName,)"
        }
    }
}