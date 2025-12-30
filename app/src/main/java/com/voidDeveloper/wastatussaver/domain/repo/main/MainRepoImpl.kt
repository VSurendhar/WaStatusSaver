package com.voidDeveloper.wastatussaver.domain.repo.main

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.voidDeveloper.wastatussaver.domain.model.AudioFile
import com.voidDeveloper.wastatussaver.domain.model.ImageFile
import com.voidDeveloper.wastatussaver.domain.model.MediaFile
import com.voidDeveloper.wastatussaver.domain.model.VideoFile


import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import javax.inject.Inject
import kotlin.apply

class MainRepoImpl @Inject constructor(@ApplicationContext applicationContext: Context) : MainRepo {

    private var resolver: ContentResolver = applicationContext.contentResolver

    private val targetMediaCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Downloads.EXTERNAL_CONTENT_URI
    }

    private val BASE_URL = "https://api.telegram.org/bot"
    private val client = OkHttpClient()

    override suspend fun saveMediaFile(mediaFile: MediaFile, onSaveCompleted: () -> Unit) {
        when (mediaFile) {
            is ImageFile -> saveImageFile(mediaFile = mediaFile, onSaveCompleted = onSaveCompleted)
            is VideoFile -> saveVideoFile(mediaFile = mediaFile, onSaveCompleted = onSaveCompleted)
            is AudioFile -> saveAudioFile(mediaFile = mediaFile, onSaveCompleted = onSaveCompleted)
            else -> {}
        }
    }

    override fun getSavedMediaFiles(): List<String> {
        val imageFiles = getMediaFromDownloads("Image")
        val videoFiles = getMediaFromDownloads("Video")
        val audioFiles = getMediaFromDownloads("Audio")
        return (imageFiles + videoFiles + audioFiles)
    }

    private fun getMediaFromDownloads(media: String): List<String> {
        val fileNames = mutableListOf<String>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Downloads.DISPLAY_NAME
        )

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
        } else {
            null
        }

        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf("%Download/WaStatusSaver/$media/%")
        } else {
            null
        }

        try {
            resolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                MediaStore.Downloads.DATE_ADDED + " DESC"
            )?.use { cursor ->

                val nameIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    fileNames.add(cursor.getString(nameIndex))
                }
            }
        } catch (e: Exception) {
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

    override suspend fun sendLogsTelegram(logs: String) {
        val token = com.voidDeveloper.wastatussaver.BuildConfig.BOT_TOKEN
        val chatId = com.voidDeveloper.wastatussaver.BuildConfig.CHAT_ID
        try {
            delay(5000)
            // Create JSON body
            val json = JSONObject().apply {
                put("chat_id", chatId)
                put("text", logs)
                put("parse_mode", "MarkdownV2")
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)

            // Build request
            val request = Request.Builder()
                .url("$BASE_URL$token/sendMessage")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            // Execute request
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()

                } else {
                    val errorBody = response.body?.string()


                }
            }

        } catch (e: IOException) {

        } catch (e: Exception) {

        }
    }
}