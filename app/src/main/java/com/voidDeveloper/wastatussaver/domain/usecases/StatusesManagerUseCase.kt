package com.voidDeveloper.wastatussaver.domain.usecases

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.voidDeveloper.wastatussaver.data.utils.Constants
import com.voidDeveloper.wastatussaver.data.utils.Constants.AUDIO_MIME_TYPE_STARTING
import com.voidDeveloper.wastatussaver.data.utils.Constants.IMAGE_MIME_TYPE_STARTING
import com.voidDeveloper.wastatussaver.data.utils.Constants.VIDEO_MIME_TYPE_STARTING
import com.voidDeveloper.wastatussaver.domain.repo.main.MainRepo
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.AudioFile
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.ImageFile
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.MediaFile
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.UnknownFile
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.VideoFile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class StatusesManagerUseCase @Inject constructor(
    private val appContext: Application,
    private val mainRepo: MainRepo,
) {


    fun hasPermission(uri: Uri?): Boolean {
        if (uri == null) {
            return false
        }
        return appContext.contentResolver.persistedUriPermissions.any { perm ->
            perm.uri == uri && perm.isReadPermission
        }
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

    fun getFiles(uri: Uri?): List<MediaFile> {
        Log.i(Constants.TAG, "getFiles: Getting Files")
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            uri, DocumentsContract.getTreeDocumentId(uri)
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
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
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

                        else -> UnknownFile(uri = fileUri)
                    }
                    resList.add(
                        mediaFile
                    )
                }
            }

        }
        return resList.toList()
    }

    fun isStatusDownloaded(fileName: String): Boolean {
        return downloadedFiles.contains(fileName)
    }

    suspend fun saveMediaFile(mediaFile: MediaFile, onSaveCompleted: () -> Unit = {}) {
        mainRepo.saveMediaFile(mediaFile = mediaFile, onSaveCompleted = onSaveCompleted)
    }

}