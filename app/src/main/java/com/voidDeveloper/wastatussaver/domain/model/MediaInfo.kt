package com.voidDeveloper.wastatussaver.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaInfo(
    val uri: String,
    val lastPlayedMillis: Long,
    val fileName: String,
) : Parcelable

val emptyMediaInfo = MediaInfo("", 0, "")