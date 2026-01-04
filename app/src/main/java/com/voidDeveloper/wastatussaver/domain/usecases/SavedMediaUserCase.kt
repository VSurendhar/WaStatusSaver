package com.voidDeveloper.wastatussaver.domain.usecases

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.domain.model.AudioFile
import com.voidDeveloper.wastatussaver.domain.model.ImageFile
import com.voidDeveloper.wastatussaver.domain.model.MediaFile
import com.voidDeveloper.wastatussaver.domain.model.MediaInfo
import com.voidDeveloper.wastatussaver.domain.model.VideoFile
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot.DownloadState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import java.io.IOException
import javax.inject.Inject

class SavedMediaHandlingUserCase @Inject constructor(@ApplicationContext context: Context) {

    private var resolver: ContentResolver = context.contentResolver


    suspend fun saveMediaFile(mediaFile: MediaFile, onSaveCompleted: () -> Unit) {
        when (mediaFile) {
            is ImageFile -> saveImageFile(mediaFile = mediaFile, onSaveCompleted = onSaveCompleted)
            is VideoFile -> saveVideoFile(mediaFile = mediaFile, onSaveCompleted = onSaveCompleted)
            is AudioFile -> saveAudioFile(mediaFile = mediaFile, onSaveCompleted = onSaveCompleted)
            else -> {}
        }
    }

    fun getSavedMediaFiles(): List<MediaInfo> {
        val result = getAllSavedMedia()
        return result
    }


    private fun getAllSavedMedia(): List<MediaInfo> {
        val result = mutableListOf<MediaInfo>()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return emptyList()
        }

        val imageCount = queryMedia(
            mediaStoreUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            relativePath = "%Download/WaStatusSaver/Image%",
            mediaType = MediaType.IMAGE
        )
        result += imageCount

        val videoCount = queryMedia(
            mediaStoreUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            relativePath = "%Download/WaStatusSaver/Video%",
            mediaType = MediaType.VIDEO
        )
        result += videoCount

        val audioCount = queryMedia(
            mediaStoreUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            relativePath = "%Download/WaStatusSaver/Audio%",
            mediaType = MediaType.AUDIO
        )
        result += audioCount

        return result.sortedByDescending { it.lastPlayedMillis }
    }

    private fun queryMedia(
        mediaStoreUri: Uri,
        relativePath: String,
        mediaType: MediaType,
    ): List<MediaInfo> {
        val list = mutableListOf<MediaInfo>()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME
        )

        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf(relativePath)

        val queryArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.MediaColumns.DATE_ADDED} DESC")
                putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
            }
        } else {
            null
        }

        try {
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && queryArgs != null) {
                resolver.query(mediaStoreUri, projection, queryArgs, null)
            } else {
                resolver.query(
                    mediaStoreUri,
                    projection,
                    selection,
                    selectionArgs,
                    "${MediaStore.MediaColumns.DATE_ADDED} DESC"
                )
            }

            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    val name = it.getString(nameIndex)
                    val uri = ContentUris.withAppendedId(mediaStoreUri, id)

                    list.add(
                        MediaInfo(
                            fileName = name,
                            uri = uri.toString(),
                            lastPlayedMillis = 0,
                            mediaType = mediaType,
                            downloadStatus = DownloadState.DOWNLOADED
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

    suspend fun saveAudioFile(
        mediaFile: AudioFile,
        onSaveCompleted: () -> Unit,
    ) = withContext(Dispatchers.IO) {

        val timeInSeconds = System.currentTimeMillis() / 1000

        val values = ContentValues().apply {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/WaStatusSaver/Audio"
            )
            put(MediaStore.MediaColumns.DISPLAY_NAME, mediaFile.fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/opus")
            put(MediaStore.MediaColumns.DATE_ADDED, timeInSeconds)
            put(MediaStore.MediaColumns.IS_PENDING, 1)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_DOWNLOAD, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            return@withContext
        }

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                resolver.openInputStream(mediaFile.uri)?.use { inputStream ->
                    inputStream.copyTo(outputStream)
                } ?: throw IOException("InputStream is null")
            } ?: throw IOException("OutputStream is null")

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            delay(400)
            onSaveCompleted()

        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    suspend fun saveImageFile(
        mediaFile: ImageFile,
        onSaveCompleted: () -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            val bitmap = uriToBitmap(mediaFile.uri) ?: return@withContext

            val timeInSeconds = System.currentTimeMillis() / 1000

            val values = ContentValues().apply {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/WaStatusSaver/Image"
                )
                put(MediaStore.MediaColumns.DISPLAY_NAME, mediaFile.fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.DATE_ADDED, timeInSeconds)
                put(MediaStore.MediaColumns.IS_PENDING, 1)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.IS_DOWNLOAD, 1)
                }
            }

            val uri = resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
            )
            if (uri == null) {
                return@withContext
            }

            try {
                resolver.openOutputStream(uri)?.use { output ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)) {
                        throw IOException("Failed to write bitmap")
                    }
                }

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

            } catch (e: Exception) {
                resolver.delete(uri, null, null)
            }

            delay(400)
            onSaveCompleted()
        }
    }

    suspend fun saveVideoFile(
        mediaFile: VideoFile,
        onSaveCompleted: () -> Unit,
    ) {
        withContext(Dispatchers.IO) {

            val timeInSeconds = System.currentTimeMillis() / 1000

            val videoContentValues = ContentValues().apply {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/WaStatusSaver/Video"
                )
                put(MediaStore.MediaColumns.DISPLAY_NAME, mediaFile.fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.DATE_ADDED, timeInSeconds)
                put(MediaStore.MediaColumns.IS_PENDING, 1)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.IS_DOWNLOAD, 1)
                }
            }

            val videoUri = resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                videoContentValues
            )
            if (videoUri == null) {
                return@withContext
            }

            try {
                resolver.openOutputStream(videoUri)?.use { outputStream ->
                    resolver.openInputStream(mediaFile.uri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    } ?: throw IllegalStateException("InputStream is null")
                } ?: throw IllegalStateException("OutputStream is null")

                videoContentValues.clear()
                videoContentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(videoUri, videoContentValues, null, null)

            } catch (e: Exception) {
                resolver.delete(videoUri, null, null)
                throw e
            }

            delay(400)
            onSaveCompleted()
        }
    }

    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = resolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }


    fun getMediaUriFromDownloads(
        fileName: String,
        mediaFolder: String,
    ): Uri? {

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME
        )

        val selection: String?
        val selectionArgs: Array<String>?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection =
                "${MediaStore.Downloads.DISPLAY_NAME} = ? AND " +
                        "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"

            selectionArgs = arrayOf(
                fileName,
                "%Download/WaStatusSaver/$mediaFolder/%"
            )
        } else {
            selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            selectionArgs = arrayOf(fileName)
        }

        return try {
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = android.os.Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                    putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                }
                resolver.query(collection, projection, queryArgs, null)
            } else {
                resolver.query(collection, projection, selection, selectionArgs, null)
            }

            cursor?.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val id = it.getLong(idIndex)
                    val uri = ContentUris.withAppendedId(collection, id)
                    uri
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }


}