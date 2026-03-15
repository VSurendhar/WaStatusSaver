package com.voiddevelopers.wastatussaver.domain.usecases

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import com.voiddevelopers.wastatussaver.data.utils.Constants
import com.voiddevelopers.wastatussaver.data.utils.Constants.AUDIO_MIME_TYPE_STARTING
import com.voiddevelopers.wastatussaver.data.utils.Constants.IMAGE_MIME_TYPE_STARTING
import com.voiddevelopers.wastatussaver.data.utils.Constants.VIDEO_MIME_TYPE_STARTING
import com.voiddevelopers.wastatussaver.data.utils.extentions.hasReadPermission
import com.voiddevelopers.wastatussaver.domain.model.AudioFile
import com.voiddevelopers.wastatussaver.domain.model.ImageFile
import com.voiddevelopers.wastatussaver.domain.model.MediaFile
import com.voiddevelopers.wastatussaver.domain.model.MediaType
import com.voiddevelopers.wastatussaver.domain.model.UnknownFile
import com.voiddevelopers.wastatussaver.domain.model.VideoFile
import com.voiddevelopers.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot.DownloadState
import javax.inject.Inject

class StatusesManagerUseCase @Inject constructor(
    val appContext: Application,
    val statusMediaHandlingUserCase: SavedMediaHandlingUserCase,
) {

    fun hasPermission(uri: Uri?): Boolean {
        return appContext.hasReadPermission(uri)
    }

    private lateinit var downloadedFiles: List<String>

    init {
        loadDownloadedFiles()
    }

    fun loadDownloadedFiles() {
        downloadedFiles = statusMediaHandlingUserCase.getSavedMediaFiles().map { it.fileName }
    }

    fun refreshDownloadedFiles() {
        downloadedFiles = statusMediaHandlingUserCase.getSavedMediaFiles().map { it.fileName }
    }

    fun getFiles(
        destinationUri: Uri?,
        preferredMediaTypes: List<MediaType>? = null,
    ): List<MediaFile> {

        if (destinationUri == null) {
            return emptyList()
        }

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            destinationUri,
            DocumentsContract.getTreeDocumentId(destinationUri)
        )

        refreshDownloadedFiles()

        val cursor: Cursor? = appContext.contentResolver.query(
            childrenUri,
            null,
            null,
            null,
            null
        )

        if (cursor == null) {
            return emptyList()
        }

        val resList = mutableListOf<MediaFile>()

        cursor.use { c ->
            val nameIndex =
                c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeTypeIndex =
                c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val docIdIndex =
                c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val flagsIndex = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_FLAGS)

            while (c.moveToNext()) {
                val name = c.getString(nameIndex)
                val mimeType = c.getString(mimeTypeIndex)
                val documentId = c.getString(docIdIndex)
                val fileUri =
                    DocumentsContract.buildDocumentUriUsingTree(destinationUri, documentId)

                val flags = c.getInt(flagsIndex)
                val isTrashed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE != 0
                } else {
                    false
                }

                if (name == Constants.NO_MEDIA) {
                    continue
                }

                val downloadState =
                    if (isStatusDownloaded(name)) DownloadState.DOWNLOADED else DownloadState.NOT_DOWNLOADED

                val mediaFile: MediaFile = when {
                    mimeType.startsWith(IMAGE_MIME_TYPE_STARTING, ignoreCase = true) -> {
                        ImageFile(
                            uri = fileUri,
                            fileName = name,
                            initialDownloadState = downloadState
                        )
                    }

                    mimeType.startsWith(VIDEO_MIME_TYPE_STARTING, ignoreCase = true) -> {
                        VideoFile(
                            uri = fileUri,
                            fileName = name,
                            initialDownloadState = downloadState
                        )
                    }

                    mimeType.startsWith(AUDIO_MIME_TYPE_STARTING, ignoreCase = true) -> {
                        AudioFile(
                            uri = fileUri,
                            fileName = name,
                            initialDownloadState = downloadState
                        )
                    }

                    else -> UnknownFile(
                        uri = fileUri,
                        fileName = "void",
                        initialDownloadState = downloadState
                    )
                }
                resList.add(
                    mediaFile
                )
            }
        }

        val filteredList = resList.filter {
            preferredMediaTypes == null || preferredMediaTypes.contains(it.mediaType)
        }

        return filteredList.toList()

    }

    fun isStatusDownloaded(fileName: String): Boolean {
        val baseName = fileName.substringBeforeLast(".")
        return downloadedFiles.any { it.startsWith(baseName) }
    }

    suspend fun saveMediaFile(mediaFile: MediaFile, onSaveCompleted: suspend () -> Unit = {}) {
        statusMediaHandlingUserCase.saveMediaFile(
            mediaFile = mediaFile,
            onSaveCompleted = onSaveCompleted
        )
    }

}