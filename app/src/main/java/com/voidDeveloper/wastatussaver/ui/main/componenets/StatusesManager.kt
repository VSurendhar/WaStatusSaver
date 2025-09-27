package com.voidDeveloper.wastatussaver.ui.main.componenets

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.voidDeveloper.wastatussaver.data.utils.Constants.TAG
import com.voidDeveloper.wastatussaver.ui.main.File
import com.voidDeveloper.wastatussaver.ui.main.FileType
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

    fun getFiles(uri: Uri?): List<File> {

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            uri, DocumentsContract.getTreeDocumentId(uri)
        )

        val cursor: Cursor? = appContext.contentResolver.query(childrenUri, null, null, null, null)
        val resList = mutableListOf<File>()
        cursor?.use { c ->

            val nameIndex = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeType = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val docIdIndex = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)

            while (c.moveToNext()) {
                val name = c.getString(nameIndex)
                val mimeType = c.getString(mimeType)
                val documentId = c.getString(docIdIndex)
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
                Log.i(TAG, "getFiles: $name $mimeType $documentId $fileUri")
                resList.add(
                    File(
                        id = documentId,
                        isDownloaded = getDownloadedStatus(),
                        uri = fileUri,
                        fileType = FileType.IMAGES
                    )
                )
            }

        }
        return resList.toList()
    }

    private fun getDownloadedStatus(): Boolean {
        return false
    }

}