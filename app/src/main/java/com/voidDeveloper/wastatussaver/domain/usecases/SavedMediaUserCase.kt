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

    private val targetMediaCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Downloads.EXTERNAL_CONTENT_URI
    }

    suspend fun saveMediaFile(mediaFile: MediaFile, onSaveCompleted: () -> Unit) {
        when (mediaFile) {
            is ImageFile -> saveImageFile(mediaFile = mediaFile, onSaveCompleted = onSaveCompleted)
            is VideoFile -> saveVideoFile(mediaFile = mediaFile, onSaveCompleted = onSaveCompleted)
            is AudioFile -> saveAudioFile(mediaFile = mediaFile, onSaveCompleted = onSaveCompleted)
            else -> {}
        }
    }

    fun getSavedMediaFiles(): List<MediaInfo> {
        val imageFiles = getMediaFromDownloads("Image")
        val videoFiles = getMediaFromDownloads("Video")
        val audioFiles = getMediaFromDownloads("Audio")
        return (imageFiles + videoFiles + audioFiles)
    }

    private fun getMediaFromDownloads(media: String): List<MediaInfo> {
        val fileNames = mutableListOf<MediaInfo>()

        val mediaType = when (media) {
            "Image" -> MediaType.IMAGE
            "Video" -> MediaType.VIDEO
            "Audio" -> MediaType.AUDIO
            else -> MediaType.UNSPECIFIED
        }

        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME
        )

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
        } else null

        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf("%Download/WaStatusSaver/$media/%")
        } else null

        try {
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Downloads.DATE_ADDED} DESC"
            )?.use { cursor ->

                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val name = cursor.getString(nameIndex)

                    val uri = ContentUris.withAppendedId(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        id
                    )

                    fileNames.add(
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
            Log.e("WaStatusSaver", "Failed to read downloads", e)
        }

        return fileNames
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
        }

        val uri = resolver.insert(targetMediaCollection, values) ?: return@withContext

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
            }

            val uri = resolver.insert(targetMediaCollection, values) ?: return@withContext

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
            }

            val videoUri = resolver.insert(
                targetMediaCollection,
                videoContentValues
            ) ?: return@withContext

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
            resolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {
                    val idIndex =
                        cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)

                    val id = cursor.getLong(idIndex)

                    ContentUris.withAppendedId(collection, id)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("WaStatusSaver", "Failed to get media URI", e)
            null
        }
    }


}