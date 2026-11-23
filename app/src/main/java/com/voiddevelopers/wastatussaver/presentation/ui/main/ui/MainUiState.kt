package com.voiddevelopers.wastatussaver.presentation.ui.main.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.voiddevelopers.wastatussaver.R
import com.voiddevelopers.wastatussaver.domain.model.MediaFile
import com.voiddevelopers.wastatussaver.domain.model.MediaInfo
import com.voiddevelopers.wastatussaver.domain.model.MediaType

data class UiState(
    val title: Title? = null,
    val appInstalled: Boolean = false,
    val selectionMode: SelectionMode? = null,
    val currentMediaType: MediaType? = null,
    val shouldShowOnBoardingUi: Boolean? = null,
    val hasSafAccessPermission: Boolean = false,
    val mediaFiles: List<MediaFile> = emptyList()
)

fun MediaType.toFileType() = when (this) {
    MediaType.AUDIO -> MediaType.AUDIO
    MediaType.IMAGE -> MediaType.IMAGE
    MediaType.VIDEO -> MediaType.VIDEO
    else -> MediaType.UNSPECIFIED
}

fun MediaFile.toDomainMediaInfo(): MediaInfo = MediaInfo(
    uri = uri.toString(),
    lastPlayedMillis = 0,
    fileName = fileName,
    mediaType = mediaType,
    downloadStatus = downloadState
)

enum class SelectionMode {
    MULTI_SELECT,
    SINGLE_SELECT
}

sealed class Title(@StringRes val resId: Int, val packageName: String, val uri: Uri) {
    object Whatsapp : Title(
        resId = R.string.title_whatsapp,
        packageName = "com.whatsapp",
        uri = (
            "content://com.android.externalstorage.documents/tree/" +
                "primary%3AAndroid%2Fmedia%2F" +
                "com.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
            ).toUri()
    ) {
        override fun toString(): String = "Whatsapp(resId=$resId, packageName=$packageName,)"
    }

    object WhatsappBusiness : Title(
        resId = R.string.title_whatsapp_business,
        packageName = "com.whatsapp.w4b",
        uri = (
            "content://com.android.externalstorage.documents/tree/" +
                "primary%3AAndroid%2Fmedia%2F" +
                "com.whatsapp.w4b%2FWhatsApp%20Business%2FMedia%2F.Statuses"
            ).toUri()
    ) {
        override fun toString(): String =
            "WhatsappBusiness(resId=$resId, packageName=$packageName,)"
    }
}

/** Value sent to analytics for this title. */
val Title.analyticsName: String
    get() = when (this) {
        Title.Whatsapp -> "whatsapp"
        Title.WhatsappBusiness -> "whatsapp_business"
    }
