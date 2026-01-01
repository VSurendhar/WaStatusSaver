package com.voidDeveloper.wastatussaver.data.utils.extentions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.util.unpackInt1
import androidx.core.net.toUri
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.datastore.proto.App
import com.voidDeveloper.wastatussaver.data.datastore.proto.AutoSaveInterval
import com.voidDeveloper.wastatussaver.data.datastore.proto.AutoSaveUserPref
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.data.datastore.proto.StatusMedia
import com.voidDeveloper.wastatussaver.data.utils.Constants.DEFAULT_AUTO_SAVE_INTERVAL
import com.voidDeveloper.wastatussaver.domain.model.AudioFile
import com.voidDeveloper.wastatussaver.domain.model.AutoSaveIntervalDomain
import com.voidDeveloper.wastatussaver.domain.model.ImageFile
import com.voidDeveloper.wastatussaver.domain.model.MediaFile
import com.voidDeveloper.wastatussaver.domain.model.MediaInfo
import com.voidDeveloper.wastatussaver.domain.model.UnknownFile
import com.voidDeveloper.wastatussaver.domain.model.VideoFile
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.Title
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.toFileType
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.DownloadState

fun Context.findActivity(): Activity? {
    if (this is Activity) {
        return this
    }
    return (this as? ContextWrapper)?.baseContext?.findActivity()
}

fun Context.isInternetAvailable(): Boolean {
    val connectivityManager =
        this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}

fun StatusMedia.toDomainMediaFile(): MediaFile {
    return when (mediaType) {
        MediaType.AUDIO -> {
            AudioFile(uri.toUri(), fileName)
        }

        MediaType.VIDEO -> {
            VideoFile(uri.toUri(), fileName)
        }

        MediaType.IMAGE -> {
            ImageFile(uri.toUri(), fileName)
        }

        else -> {
            UnknownFile(uri.toUri(), fileName = fileName)
        }
    }
}

fun StatusMedia.toDomainMediaInfo(): MediaInfo {
    return MediaInfo(
        uri = uri,
        lastPlayedMillis = 0,
        fileName = fileName,
        mediaType = mediaType.toFileType(),
        downloadStatus = DownloadState.NOT_DOWNLOADED
    )
}


fun Context.fileNotFoundBitmap(): Bitmap =
    BitmapFactory.decodeResource(resources, R.drawable.img_loading_placeholder)

fun AutoSaveUserPref.getInterval(): Int {
    return when (autoSaveInterval) {
        AutoSaveInterval.ONE -> 1
        AutoSaveInterval.SIX -> 6
        AutoSaveInterval.TWELVE -> 12
        AutoSaveInterval.TWENTY_FOUR -> 24
        AutoSaveInterval.UNRECOGNIZED, AutoSaveInterval.UNKOWN -> 0
    }
}

fun Title.toApp(): App {
    return when (this) {
        Title.Whatsapp -> App.WHATSAPP
        Title.WhatsappBusiness -> App.WHATSAPP_BUSINESS
    }
}

fun App.toTitle(): Title {
    return when (this) {
        App.WHATSAPP -> Title.Whatsapp
        App.WHATSAPP_BUSINESS -> Title.WhatsappBusiness
        else -> Title.Whatsapp
    }
}

fun AutoSaveInterval.toAutoSaveIntervalDomain(): AutoSaveIntervalDomain? {
    return when (this) {
        AutoSaveInterval.UNKOWN -> null
        AutoSaveInterval.ONE -> AutoSaveIntervalDomain.ONE_HOUR
        AutoSaveInterval.SIX -> AutoSaveIntervalDomain.SIX_HOURS
        AutoSaveInterval.TWELVE -> AutoSaveIntervalDomain.TWELVE_HOURS
        AutoSaveInterval.TWENTY_FOUR -> AutoSaveIntervalDomain.TWENTY_FOUR_HOURS
        AutoSaveInterval.UNRECOGNIZED -> null
    }
}

fun AutoSaveIntervalDomain?.safeAutoSaveIntervalDomain(): AutoSaveIntervalDomain {
    return when (this) {
        null -> AutoSaveIntervalDomain.TWENTY_FOUR_HOURS
        else -> this
    }
}

fun AutoSaveUserPref?.string(): String {
    return "App ${this?.app}, Enable ${this?.enable}, Interval ${this?.autoSaveInterval}, MediaList ${this?.mediaTypeList}"
}


fun Modifier.singleClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    var clicked by remember { mutableStateOf(false) }

    this.then(
        Modifier.clickable(
            enabled = enabled && !clicked,
            onClick = {
                clicked = true
                onClick()
            }
        )
    )
}