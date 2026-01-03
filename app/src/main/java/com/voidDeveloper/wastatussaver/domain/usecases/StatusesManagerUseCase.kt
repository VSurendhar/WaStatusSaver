package com.voidDeveloper.wastatussaver.domain.usecases

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.data.utils.Constants
import com.voidDeveloper.wastatussaver.data.utils.Constants.AUDIO_MIME_TYPE_STARTING
import com.voidDeveloper.wastatussaver.data.utils.Constants.IMAGE_MIME_TYPE_STARTING
import com.voidDeveloper.wastatussaver.data.utils.Constants.VIDEO_MIME_TYPE_STARTING
import com.voidDeveloper.wastatussaver.data.utils.extentions.hasReadPermission
import com.voidDeveloper.wastatussaver.domain.model.AudioFile
import com.voidDeveloper.wastatussaver.domain.model.ImageFile
import com.voidDeveloper.wastatussaver.domain.model.MediaFile
import com.voidDeveloper.wastatussaver.domain.model.UnknownFile
import com.voidDeveloper.wastatussaver.domain.model.VideoFile
import com.voidDeveloper.wastatussaver.domain.repo.main.MainRepo
import javax.inject.Inject

class StatusesManagerUseCase @Inject constructor(
    private val appContext: Application,
    private val mainRepo: MainRepo,
) {

    fun hasPermission(uri: Uri?): Boolean {
        return appContext.hasReadPermission(uri)
    }

    private lateinit var downloadedFiles: List<String>

    init {
        loadDownloadedFiles()
    }

    fun loadDownloadedFiles() {
        downloadedFiles = mainRepo.getSavedMediaFiles()
    }

    fun refreshDownloadedFiles() {
        downloadedFiles = mainRepo.getSavedMediaFiles()
    }

    fun getFiles(
        destinationUri: Uri?,
        preferredMediaTypes: List<MediaType>? = null,
    ): List<MediaFile> {
        Log.i(Constants.TAG, "getFiles: Getting Files")
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            destinationUri, DocumentsContract.getTreeDocumentId(destinationUri)
        )
        refreshDownloadedFiles()
        val cursor: Cursor? = appContext.contentResolver.query(childrenUri, null, null, null, null)
        val resList = mutableListOf<MediaFile>()
        cursor?.use { c ->

            val nameIndex = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeTypeIndex = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val docIdIndex = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)

            while (c.moveToNext()) {
                val name = c.getString(nameIndex)
                val mimeType = c.getString(mimeTypeIndex)
                val documentId = c.getString(docIdIndex)
                val fileUri =
                    DocumentsContract.buildDocumentUriUsingTree(destinationUri, documentId)
                Log.i(Constants.TAG, "getFiles: $name $mimeType $documentId $fileUri")
                if (name != Constants.NO_MEDIA) {
                    val mediaFile: MediaFile = when {
                        mimeType.startsWith(IMAGE_MIME_TYPE_STARTING, ignoreCase = true) -> {
                            ImageFile(uri = fileUri, fileName = name).apply {
                                isDownloaded = isStatusDownloaded(fileName)
                            }
                        }

                        mimeType.startsWith(VIDEO_MIME_TYPE_STARTING, ignoreCase = true) -> {
                            VideoFile(uri = fileUri, fileName = name).apply {
                                isDownloaded = isStatusDownloaded(fileName)
                            }
                        }

                        mimeType.startsWith(AUDIO_MIME_TYPE_STARTING, ignoreCase = true) -> {
                            AudioFile(fileName = name, uri = fileUri).apply {
                                isDownloaded = isStatusDownloaded(fileName)
                            }
                        }

                        else -> UnknownFile(uri = fileUri, fileName = "void")
                    }
                    resList.add(
                        mediaFile
                    )
                }
            }

        }

        val filteredList =
            resList.filter { preferredMediaTypes == null || preferredMediaTypes.contains(it.mediaType) }
        return filteredList.toList()

    }

    fun isStatusDownloaded(fileName: String): Boolean {
        return downloadedFiles.contains(fileName)
    }

    suspend fun saveMediaFile(mediaFile: MediaFile, onSaveCompleted: () -> Unit = {}) {
        mainRepo.saveMediaFile(mediaFile = mediaFile, onSaveCompleted = onSaveCompleted)
    }

}