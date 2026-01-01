package com.voidDeveloper.wastatussaver.domain.usecases

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.graphics.scale
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.data.utils.compressBitmapQuality
import com.voidDeveloper.wastatussaver.data.utils.extentions.fileNotFoundBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ThumbnailMakerUseCase @Inject constructor(@ApplicationContext private val context: Context) {

    private val fileNotFoundBitmap = context.fileNotFoundBitmap()
    private val resources = context.resources

    fun getImageThumbnailFromUri(imageUri: Uri, quality: Int = 80): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.setTargetSize(150, 300)
            }.compressBitmapQuality()
        } else {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            bitmap.scale(150, 300).compressBitmapQuality()
        }
    }

    fun getVideoThumbnailFromUri(videoUri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)
            retriever.getFrameAtTime(
                0,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    private fun getAudioThumbnail(): Bitmap? {
        return BitmapFactory.decodeResource(resources, R.drawable.img_loading_placeholder)
    }

    fun getThumbnailFromUri(mediaType: MediaType, uri: Uri): Bitmap {
        return when (mediaType) {
            MediaType.IMAGE -> {
                getImageThumbnailFromUri(uri) ?: fileNotFoundBitmap
            }

            MediaType.VIDEO -> {
                getVideoThumbnailFromUri(uri) ?: fileNotFoundBitmap
            }

            MediaType.AUDIO -> {
                getAudioThumbnail() ?: fileNotFoundBitmap
            }

            else -> {
                fileNotFoundBitmap
            }
        }
    }

}