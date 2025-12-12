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
import com.voidDeveloper.wastatussaver.data.utils.Constants.TAG
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.AudioFile
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.ImageFile
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.MediaFile
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.VideoFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import javax.inject.Inject

class MainRepoImpl @Inject constructor(@ApplicationContext applicationContext: Context) : MainRepo {

    private var resolver: ContentResolver = applicationContext.contentResolver

    private val targetMediaCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Downloads.EXTERNAL_CONTENT_URI
    }

    private val destinationMediaCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Downloads.EXTERNAL_CONTENT_URI
    }


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
        val imageUriList = mutableListOf<String>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME
        )

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
        } else {
            null
        }

        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf("%Download/WaStatusSaver/$media%")
        } else {
            null
        }

        Log.d("Surendhar TAG", "Querying Downloads folder for Image")

        try {
            resolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)

                Log.d("Surendhar TAG", "Found ${cursor.count} files")

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameColumn)
                    imageUriList.add(name)

                    Log.d("Surendhar TAG", "File: $name")
                }
            }
        } catch (e: Exception) {
            Log.e("Surendhar TAG", "Error querying downloads: ${e.message}", e)
        }

        Log.d("Surendhar TAG", "Total images retrieved: ${imageUriList.size}")
        return imageUriList
    }

    suspend fun saveAudioFile(mediaFile: AudioFile, onSaveCompleted: () -> Unit) {
        withContext(Dispatchers.IO) {
            var audioFile: File? = null
            val timeInMillis = System.currentTimeMillis()

            try {
                println("I am started")
                audioFile = File(mediaFile.uri.path!!)
                println("I am ended")
            } catch (e: Exception) {
                Log.e(TAG, "saveAudioFile: Thrown Error ")
                e.printStackTrace()
                throw e
            }

            val audioContentValues = ContentValues().apply {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/WaStatusSaver/Audio"
                )
                put(MediaStore.Downloads.DISPLAY_NAME, mediaFile.fileName)
                put(MediaStore.Downloads.MIME_TYPE, "audio/opus")
                put(MediaStore.Downloads.DATE_ADDED, timeInMillis)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val audioMediaStoreUri: Uri? =
                resolver.insert(targetMediaCollection, audioContentValues)

            audioMediaStoreUri?.let {
                try {
                    Log.d("Surendhar TAG", "Starting file copy to MediaStore")
                    Log.d("Surendhar TAG", "Source file: ${audioFile.absolutePath}")
                    Log.d("Surendhar TAG", "Destination URI: $audioMediaStoreUri")

                    try {
                        resolver.openOutputStream(audioMediaStoreUri)?.use { outputStream ->
                            Log.d("Surendhar TAG", "Output stream opened successfully")

                            resolver.openInputStream(mediaFile.uri)?.use { inputStream ->
                                Log.d("Surendhar TAG", "Input stream opened successfully")
                                Log.d("Surendhar TAG", "File size: ${audioFile.length()} bytes")

                                val bytesCopied = inputStream.copyTo(outputStream)
                                Log.d("Surendhar TAG", "Copy completed: $bytesCopied bytes written")
                            } ?: Log.e("Surendhar TAG", "Failed to open input stream")
                        } ?: Log.e("Surendhar TAG", "Failed to open output stream")

                        Log.d(
                            "Surendhar TAG",
                            "Clearing content values and updating IS_PENDING flag"
                        )
                        audioContentValues.clear()
                        audioContentValues.put(MediaStore.Downloads.IS_PENDING, 0)

                        val rowsUpdated =
                            resolver.update(audioMediaStoreUri, audioContentValues, null, null)
                        Log.d("Surendhar TAG", "MediaStore updated: $rowsUpdated row(s) affected")
                        Log.d("Surendhar TAG", "File successfully saved and made available")

                    } catch (e: Exception) {
                        Log.e("Surendhar TAG", "Error during file copy: ${e.message}", e)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "saveAudioFile: Thrown Error ${e.message}")
                    e.printStackTrace()
                    resolver.delete(audioMediaStoreUri, null, null)
                }
                delay(400)
                onSaveCompleted()
            }
        }
    }

    suspend fun saveImageFile(mediaFile: ImageFile, onSaveCompleted: () -> Unit) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "saveImageFile: Starting to save image - URI: ${mediaFile.uri}")

            val bitmap = uriToBitmap(mediaFile.uri)
            Log.d(
                TAG,
                "saveImageFile: Bitmap conversion result - ${if (bitmap != null) "Success (${bitmap.width}x${bitmap.height})" else "Failed (null)"}"
            )

            val timeInMillis = System.currentTimeMillis()
            val imageFile = File(mediaFile.uri.path!!)
            Log.d(TAG, "saveImageFile: Image file path: ${imageFile.absolutePath}")

            val imageContentValues = ContentValues().apply {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/WaStatusSaver/Image"
                )
                put(MediaStore.Downloads.DISPLAY_NAME, mediaFile.fileName)
                put(MediaStore.Downloads.MIME_TYPE, "image/jpeg")
                put(MediaStore.Downloads.DATE_ADDED, timeInMillis)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            Log.d(TAG, "saveImageFile: ContentValues prepared - Name: ${timeInMillis}_image.jpeg")

            val imageMediaStoreUri: Uri? =
                resolver.insert(targetMediaCollection, imageContentValues)
            Log.d(TAG, "saveImageFile: MediaStore insert result - URI: $imageMediaStoreUri")

            imageMediaStoreUri?.let {
                try {
                    Log.d(TAG, "saveImageFile: Opening output stream for writing")
                    resolver.openOutputStream(imageMediaStoreUri)?.use { outputStream ->
                        val compressed = bitmap?.compress(
                            Bitmap.CompressFormat.JPEG, 100, outputStream
                        )
                        Log.d(TAG, "saveImageFile: Bitmap compression result: $compressed")
                    }

                    Log.d(TAG, "saveImageFile: Updating IS_PENDING flag to 0")
                    imageContentValues.clear()
                    imageContentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    val updated =
                        resolver.update(imageMediaStoreUri, imageContentValues, null, null)
                    Log.d(TAG, "saveImageFile: Update result - Rows affected: $updated")

                    Log.i(TAG, "saveImageFile: Image saved successfully - URI: $imageMediaStoreUri")
                } catch (e: Exception) {
                    Log.e(TAG, "saveImageFile: Error occurred during save operation", e)
                    e.printStackTrace()

                    Log.w(TAG, "saveImageFile: Attempting to delete failed entry")
                    val deleted = resolver.delete(imageMediaStoreUri, null, null)
                    Log.w(TAG, "saveImageFile: Delete result - Rows deleted: $deleted")
                }
                delay(400)
                onSaveCompleted()
            } ?: run {
                Log.e(TAG, "saveImageFile: Failed to insert into MediaStore - URI is null")
            }

            Log.d(TAG, "saveImageFile: Function completed")
        }
    }

    suspend fun saveVideoFile(mediaFile: VideoFile, onSaveCompleted: () -> Unit) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "saveVideoFile: Starting to save video - URI: ${mediaFile.uri}")

            val timeInMillis = System.currentTimeMillis()
            val videoFile = File(mediaFile.uri.path!!)
            Log.d(TAG, "saveVideoFile: Video file path: ${videoFile.absolutePath}")
            Log.d(
                TAG,
                "saveVideoFile: Video file exists: ${videoFile.exists()}, Size: ${videoFile.length()} bytes"
            )

            val videoContentValues = ContentValues().apply {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/WaStatusSaver/Video"
                )
                put(MediaStore.Downloads.DISPLAY_NAME, mediaFile.fileName)
                put(MediaStore.Downloads.MIME_TYPE, "video/mp4")
                put(MediaStore.Downloads.DATE_ADDED, timeInMillis)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            Log.d(TAG, "saveVideoFile: ContentValues prepared - Name: ${timeInMillis}_video.mp4")

            val videoMediaStoreUri: Uri? =
                resolver.insert(targetMediaCollection, videoContentValues)
            Log.d(TAG, "saveVideoFile: MediaStore insert result - URI: $videoMediaStoreUri")

            videoMediaStoreUri?.let {
                try {
                    Log.d(TAG, "saveVideoFile: Opening streams for file copy operation")
                    var bytesCopied = 0L

                    resolver.openOutputStream(videoMediaStoreUri)?.use { outputStream ->
                        Log.d(TAG, "saveVideoFile: Output stream opened successfully")

                        resolver.openInputStream(mediaFile.uri)?.use { inputStream ->
                            Log.d(TAG, "saveVideoFile: Input stream opened successfully")

                            bytesCopied = inputStream.copyTo(outputStream)
                            Log.d(
                                TAG,
                                "saveVideoFile: File copy completed - Bytes copied: $bytesCopied"
                            )
                        } ?: run {
                            Log.e(TAG, "saveVideoFile: Failed to open input stream")
                        }
                    } ?: run {
                        Log.e(TAG, "saveVideoFile: Failed to open output stream")
                    }

                    Log.d(TAG, "saveVideoFile: Updating IS_PENDING flag to 0")
                    videoContentValues.clear()
                    videoContentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    val updated =
                        resolver.update(videoMediaStoreUri, videoContentValues, null, null)
                    Log.d(TAG, "saveVideoFile: Update result - Rows affected: $updated")

                    Log.i(
                        TAG,
                        "saveVideoFile: Video saved successfully - URI: $videoMediaStoreUri, Size: $bytesCopied bytes"
                    )

                } catch (e: Exception) {
                    Log.e(TAG, "saveVideoFile: Error occurred during save operation", e)
                    Log.e(
                        TAG,
                        "saveVideoFile: Error type: ${e.javaClass.simpleName}, Message: ${e.message}"
                    )
                    e.printStackTrace()

                    Log.w(TAG, "saveVideoFile: Attempting to delete failed entry")
                    val deleted = resolver.delete(videoMediaStoreUri, null, null)
                    Log.w(TAG, "saveVideoFile: Delete result - Rows deleted: $deleted")
                }
                delay(400)
                onSaveCompleted()
            } ?: run {
                Log.e(TAG, "saveVideoFile: Failed to insert into MediaStore - URI is null")
            }

            Log.d(TAG, "saveVideoFile: Function completed")
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


}