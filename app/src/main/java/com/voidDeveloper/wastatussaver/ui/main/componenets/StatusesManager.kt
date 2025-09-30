package com.voidDeveloper.wastatussaver.ui.main.componenets

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.voidDeveloper.wastatussaver.data.utils.Constants.NO_MEDIA
import com.voidDeveloper.wastatussaver.data.utils.Constants.TAG
import com.voidDeveloper.wastatussaver.ui.main.AudioFile
import com.voidDeveloper.wastatussaver.ui.main.ImageFile
import com.voidDeveloper.wastatussaver.ui.main.MediaFile
import com.voidDeveloper.wastatussaver.ui.main.UnknownFile
import com.voidDeveloper.wastatussaver.ui.main.VideoFile
import javax.inject.Inject

class StatusesManager @Inject constructor(
    private val appContext: Application,
) {
    fun hasPermission(uri: Uri?): Boolean {
        if (uri == null) {
            return false
        }
        return appContext.contentResolver.persistedUriPermissions.any { perm ->
            perm.uri == uri && perm.isReadPermission
        }
    }

    fun getFiles(uri: Uri?): List<MediaFile> {

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            uri, DocumentsContract.getTreeDocumentId(uri)
        )

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
                Log.i(TAG, "getFiles: $name $mimeType $documentId $fileUri")
                if (name != NO_MEDIA) {
                    val mediaFile: MediaFile = when {
                        mimeType.startsWith("Image", ignoreCase = true) -> {
                            ImageFile(uri = fileUri).apply { isDownloaded = getDownloadedStatus() }
                        }

                        mimeType.startsWith("Video", ignoreCase = true) -> {
                            VideoFile(uri = fileUri).apply { isDownloaded = getDownloadedStatus() }
                        }

                        mimeType.startsWith("Audio", ignoreCase = true) -> {
                            AudioFile(name = name, uri = fileUri).apply {
                                isDownloaded = getDownloadedStatus()
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

    private fun getDownloadedStatus(): Boolean {
        return false
    }

}