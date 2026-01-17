package com.voidDeveloper.wastatussaver.domain.usecases

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
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
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot.DownloadState
import javax.inject.Inject

class StatusesManagerUseCase @Inject constructor(
    private val appContext: Application,
    private val statusMediaHandlingUserCase: SavedMediaHandlingUserCase
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

    fun addDownloadedFileToCache(mediaFile: MediaFile) {
        downloadedFiles = downloadedFiles + mediaFile.fileName
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

            while (c.moveToNext()) {
                val name = c.getString(nameIndex)
                val mimeType = c.getString(mimeTypeIndex)
                val documentId = c.getString(docIdIndex)
                val fileUri =
                    DocumentsContract.buildDocumentUriUsingTree(destinationUri, documentId)

                if (name == Constants.NO_MEDIA) {
                    continue
                }

                val mediaFile: MediaFile = when {
                    mimeType.startsWith(IMAGE_MIME_TYPE_STARTING, ignoreCase = true) -> {
                        ImageFile(uri = fileUri, fileName = name).apply {
                            downloadState = if(isStatusDownloaded(fileName)) DownloadState.DOWNLOADED else DownloadState.NOT_DOWNLOADED
                        }
                    }

                    mimeType.startsWith(VIDEO_MIME_TYPE_STARTING, ignoreCase = true) -> {
                        VideoFile(uri = fileUri, fileName = name).apply {
                            downloadState = if(isStatusDownloaded(fileName)) DownloadState.DOWNLOADED else DownloadState.NOT_DOWNLOADED
                        }
                    }

                    mimeType.startsWith(AUDIO_MIME_TYPE_STARTING, ignoreCase = true) -> {
                        AudioFile(uri = fileUri, fileName = name).apply {
                            downloadState = if(isStatusDownloaded(fileName)) DownloadState.DOWNLOADED else DownloadState.NOT_DOWNLOADED
                        }
                    }

                        else -> UnknownFile(uri = fileUri, fileName = "void")
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

    suspend fun saveMediaFile(mediaFile: MediaFile, onSaveCompleted: () -> Unit = {}) {
        statusMediaHandlingUserCase.saveMediaFile(mediaFile = mediaFile, onSaveCompleted = onSaveCompleted)
    }

}