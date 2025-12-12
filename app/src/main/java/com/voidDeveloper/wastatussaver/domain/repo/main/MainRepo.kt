package com.voidDeveloper.wastatussaver.domain.repo.main

import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.MediaFile

interface MainRepo {
    suspend fun saveMediaFile(mediaFile: MediaFile, onSaveCompleted: () -> Unit)
    fun getSavedMediaFiles(): List<String>
}