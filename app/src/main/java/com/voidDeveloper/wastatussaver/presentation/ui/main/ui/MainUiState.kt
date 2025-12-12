package com.voidDeveloper.wastatussaver.presentation.ui.main.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.voidDeveloper.wastatussaver.R

data class UiState(
    val title: Title? = null,
    val appInstalled: Boolean? = null,
    val selectionMode: SelectionMode? = null,
    val currentFileType: FileType? = null,
    val shouldShowOnBoardingUi: Boolean? = null,
    val hasSafAccessPermission: Boolean? = null,
    val mediaFiles: List<MediaFile> = emptyList(),
    val onGoingDownload: MutableList<MediaFile> = mutableListOf(),
    val showAutoSaveDialog: Boolean = false,
    val showNotificationPermissionDialog: Boolean = false,
    val showNotificationPermissionSettingsDialog: Boolean = false,
    val savedAutoSaveInterval: Int = 1,
)

enum class FileType {
    IMAGES, VIDEOS, AUDIO, UNSPECIFIED
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