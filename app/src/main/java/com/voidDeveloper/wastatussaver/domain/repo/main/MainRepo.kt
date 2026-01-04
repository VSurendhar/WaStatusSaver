package com.voidDeveloper.wastatussaver.domain.repo.main

import com.voidDeveloper.wastatussaver.domain.model.MediaFile


interface MainRepo {
//    suspend fun saveMediaFile(mediaFile: MediaFile, onSaveCompleted: () -> Unit)
//    fun getSavedMediaFiles(): List<String>
    suspend fun sendLogsTelegram(logs: String)
}