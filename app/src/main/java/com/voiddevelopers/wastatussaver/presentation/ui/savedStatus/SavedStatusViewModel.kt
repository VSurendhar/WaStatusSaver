package com.voiddevelopers.wastatussaver.presentation.ui.savedStatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiddevelopers.wastatussaver.core.analytics.AnalyticsEvents
import com.voiddevelopers.wastatussaver.core.analytics.AnalyticsProperties
import com.voiddevelopers.wastatussaver.core.analytics.AppAnalytics
import com.voiddevelopers.wastatussaver.domain.model.MediaFile
import com.voiddevelopers.wastatussaver.domain.model.MediaType
import com.voiddevelopers.wastatussaver.domain.model.toMediaFile
import com.voiddevelopers.wastatussaver.domain.usecases.SavedMediaHandlingUserCase
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SavedStatusViewModel @Inject constructor(
    private val savedMediaHandlingUserCase: SavedMediaHandlingUserCase,
    private val appAnalytics: AppAnalytics,
) : ViewModel() {

    private val _savedStatus = MutableStateFlow<List<MediaFile>>(emptyList())
    val savedStatus = _savedStatus.asStateFlow()

    fun onScreenOpened() {
        appAnalytics.track(AnalyticsEvents.VIEWED_SAVED_STATUS_SCREEN)
    }

    fun onScreenClosed() {
        appAnalytics.track(AnalyticsEvents.CLOSED_SAVED_STATUS_SCREEN)
    }

    fun onTabChanged(tab: MediaType) {
        trackTab(AnalyticsEvents.VIEWED_MEDIA_TAB_SAVED_STATUS, tab)
    }

    private fun trackTab(eventName: String, tab: MediaType) {
        appAnalytics.track(
            eventName,
            mapOf(AnalyticsProperties.MEDIA_TYPE to tab.name.lowercase())
        )
    }

    fun getSavedStatusMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaFiles = savedMediaHandlingUserCase.getSavedMediaFiles().map {
                it.toMediaFile()
            }
            _savedStatus.value = mediaFiles
        }
    }

}