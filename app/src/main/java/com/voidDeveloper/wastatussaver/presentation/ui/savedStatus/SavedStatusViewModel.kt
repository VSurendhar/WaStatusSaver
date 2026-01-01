package com.voidDeveloper.wastatussaver.presentation.ui.savedStatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidDeveloper.wastatussaver.domain.model.MediaFile
import com.voidDeveloper.wastatussaver.domain.model.toMediaFile
import com.voidDeveloper.wastatussaver.domain.usecases.SavedMediaHandlingUserCase
import com.voidDeveloper.wastatussaver.domain.usecases.ThumbnailMakerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SavedStatusViewModel @Inject constructor(
    private val thumbnailMakerUseCase: ThumbnailMakerUseCase,
    private val savedMediaHandlingUserCase: SavedMediaHandlingUserCase,
) : ViewModel() {

    private val _savedStatus = MutableStateFlow<List<MediaFile>>(emptyList())
    val savedStatus = _savedStatus.asStateFlow()

    fun getSavedStatusMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaFiles = savedMediaHandlingUserCase.getSavedMediaFiles().map {
                it.toMediaFile()
            }
            _savedStatus.value = mediaFiles
        }
    }

    fun getThumbnail(mediaFile: MediaFile) =
        thumbnailMakerUseCase.getThumbnailFromUri(mediaFile.mediaType, mediaFile.uri)

}