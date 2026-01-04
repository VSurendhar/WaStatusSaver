package com.voidDeveloper.wastatussaver.domain.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.data.datastore.proto.StatusMedia
import com.voidDeveloper.wastatussaver.data.utils.compressBitmapQuality
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
    var thumbnailBitmap by mutableStateOf<Bitmap?>(null)
    abstract fun getThumbNailBitMap(context: Context): Bitmap?
}

@Suppress("DEPRECATION")
data class ImageFile(
    override val uri: Uri,
    override val fileName: String,
    override val mediaType: MediaType = MediaType.IMAGE,
) : MediaFile() {
    override fun getThumbNailBitMap(context: Context): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            thumbnailBitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.setTargetSize(width, height)
            }.compressBitmapQuality()
        } else {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            thumbnailBitmap = bitmap.scale(width, height).compressBitmapQuality()
        }
        return thumbnailBitmap
    }

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

    override fun getThumbNailBitMap(context: Context): Bitmap? {
        if (thumbnailBitmap != null) {
            return thumbnailBitmap
        }
        val bitmap = try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_audio_thumbnail)

            val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            canvas.drawColor(0x1A007F68)

            drawable?.let {
                val drawableWidth =
                    if (it.intrinsicWidth > 0) drawable.intrinsicWidth else width
                val drawableHeight =
                    if (it.intrinsicHeight > 0) drawable.intrinsicHeight else height

                val scaleX = (width.toFloat() / drawableWidth) * 0.8f
                val scaleY = height.toFloat() / drawableHeight
                val scale = minOf(scaleX, scaleY, 1f)

                val scaledWidth = (drawableWidth * scale).toInt()
                val scaledHeight = (drawableHeight * scale).toInt()

                val left = (width - scaledWidth) / 2
                val top = (height - scaledHeight) / 2
                val right = left + scaledWidth
                val bottom = top + scaledHeight

                it.setBounds(left, top, right, bottom)
                it.draw(canvas)
            }

            resultBitmap
        } catch (e: Exception) {
            null
        }
        thumbnailBitmap = bitmap
        return bitmap
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

    override fun getThumbNailBitMap(context: Context): Bitmap? {
        if (thumbnailBitmap != null) {
            return thumbnailBitmap
        }
        val retriever = MediaMetadataRetriever()
        val bitmap = try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.compressBitmapQuality()
        } catch (e: Exception) {
            e.printStackTrace()
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_failed_document)
            val bitmap = (drawable as BitmapDrawable).bitmap
            bitmap.compressBitmapQuality()
        } finally {
            retriever.release()
        }
        thumbnailBitmap = bitmap
        return bitmap
    }


}

data class UnknownFile(
    override val uri: Uri,
    override val mediaType: MediaType = MediaType.UNSPECIFIED, override val fileName: String,
) : MediaFile() {
    override fun getThumbNailBitMap(context: Context): Bitmap? {
        if (thumbnailBitmap != null) {
            return thumbnailBitmap
        }
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_failed_document)
        val bitmap = (drawable as BitmapDrawable).bitmap
        thumbnailBitmap = bitmap
        return bitmap
    }
}

fun MediaFile.toStatusMedia(): StatusMedia {
    return StatusMedia.newBuilder()
        .setUri(uri.toString())
        .setFileName(fileName)
        .setMediaType(mediaType)
        .build()
}