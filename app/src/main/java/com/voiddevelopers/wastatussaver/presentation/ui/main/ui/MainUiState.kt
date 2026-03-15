package com.voiddevelopers.wastatussaver.presentation.ui.main.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.voiddevelopers.wastatussaver.R
import com.voiddevelopers.wastatussaver.domain.model.MediaFile
import com.voiddevelopers.wastatussaver.domain.model.MediaInfo
import com.voiddevelopers.wastatussaver.domain.model.MediaType
import java.lang.reflect.Type

data class UiState(
    val title: Title? = null,
    val appInstalled: Boolean? = null,
    val selectionMode: SelectionMode? = null,
    val currentMediaType: MediaType? = null,
    val shouldShowOnBoardingUi: Boolean? = null,
    val hasSafAccessPermission: Boolean? = null,
    val mediaFiles: List<MediaFile> = emptyList(),
    val showNotificationPermissionDialog: Boolean = false,
    val showNotificationPermissionSettingsDialog: Boolean = false
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
        fileName = fileName,
        mediaType = mediaType,
        downloadStatus = downloadState
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

object TitleTypeAdapter : JsonSerializer<Title>, JsonDeserializer<Title> {

    override fun serialize(src: Title, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(
            when (src) {
                is Title.Whatsapp -> "whatsapp"
                is Title.WhatsappBusiness -> "whatsapp_business"
            }
        )
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Title {
        return when (json.asString) {
            "whatsapp" -> Title.Whatsapp
            "whatsapp_business" -> Title.WhatsappBusiness
            else -> Title.Whatsapp // fallback
        }
    }
}