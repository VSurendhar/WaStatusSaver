package com.voidDeveloper.wastatussaver.data.utils.extentions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.voidDeveloper.wastatussaver.R

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

fun Context.fileNotFoundBitmap(): Bitmap =
    BitmapFactory.decodeResource(resources, R.drawable.img_loading_placeholder)
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

fun Context.hasReadPermission(uri: Uri?): Boolean {
    if (uri == null) {
        return false
    }
    return contentResolver.persistedUriPermissions.any { perm ->
        perm.uri == uri && perm.isReadPermission
    }
}