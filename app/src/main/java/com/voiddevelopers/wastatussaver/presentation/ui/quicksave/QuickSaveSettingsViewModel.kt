package com.voiddevelopers.wastatussaver.presentation.ui.quicksave

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.voiddevelopers.wastatussaver.data.model.QuickSaveUserPref
import com.voiddevelopers.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManager
import com.voiddevelopers.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManagerImpl.DataStoreKeys.KEY_QUICK_SAVE_USER_PREF
import com.voiddevelopers.wastatussaver.domain.model.MediaType
import com.voiddevelopers.wastatussaver.domain.usecases.AppInstallCheckerUseCase
import com.voiddevelopers.wastatussaver.presentation.service.QuickSaveNotificationService
import com.voiddevelopers.wastatussaver.presentation.ui.main.ui.Title
import com.voiddevelopers.wastatussaver.presentation.ui.main.ui.TitleTypeAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuickSaveSettingsViewModel @Inject constructor(
    private val appInstallCheckerUseCase: AppInstallCheckerUseCase,
    private val dataStorePreferenceManager: DataStorePreferenceManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickSaveSettingsUiState())

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isQuickSaveEnabled = QuickSaveNotificationService.isRunning,
                    selectedTitle = getPreferredTitle(),
                    selectedMediaTypes = getQuickSaveMediaType().toSet()
                )
            }
        }
    }

    val uiState: StateFlow<QuickSaveSettingsUiState> = _uiState.asStateFlow()

    private val _effect = Channel<QuickSaveAction>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private suspend fun getQuickSaveMediaType(): List<MediaType> {
        val json = dataStorePreferenceManager
            .getPreference(KEY_QUICK_SAVE_USER_PREF, "")
            .first()

        Log.d("QuickSave", "Fetched JSON: $json")

        if (json.isEmpty()) {
            Log.d("QuickSave", "JSON is empty, returning empty list")
            return emptyList()
        }

        return try {
            val gson = GsonBuilder()
                .registerTypeHierarchyAdapter(Title::class.java, TitleTypeAdapter)
                .create()

            val pref = gson.fromJson(json, QuickSaveUserPref::class.java)

            Log.d("QuickSave", "Parsed Pref: $pref")
            Log.d("QuickSave", "Media Types: ${pref.mediaType}")

            pref.mediaType ?: emptyList()

        } catch (e: Exception) {
            Log.e("QuickSave", "Error parsing JSON", e)
            emptyList()
        }
    }

    suspend fun getPreferredTitle(): Title {
        val json = dataStorePreferenceManager.getPreference(KEY_QUICK_SAVE_USER_PREF, "").first()
        if (json.isEmpty()) return Title.Whatsapp
        return try {
            val gson =
                GsonBuilder().registerTypeHierarchyAdapter(Title::class.java, TitleTypeAdapter)
                    .create()
            val pref = gson.fromJson(json, QuickSaveUserPref::class.java)
            pref.app ?: Title.Whatsapp
        } catch (e: Exception) {
            Title.Whatsapp
        }
    }

    fun onEvent(event: QuickSaveEvent) {
        when (event) {

            is QuickSaveEvent.ShowRationale -> {
                viewModelScope.launch {
                    _effect.send(QuickSaveAction.ShowNotificationRationale)
                }
            }

            is QuickSaveEvent.OnQuickSaveSettingsChanged -> {
                viewModelScope.launch {
                    _uiState.update {
                        val title = event.title
                        val fileType = event.fileType
                        val enabled = event.isEnabled

                        if (title != null) {
                            val isInstalled =
                                appInstallCheckerUseCase.isInstalled(title.packageName)
                            val hasSafPermission =
                                appInstallCheckerUseCase.hasSafAccessPermission(title.uri)

                            if (!isInstalled) {
                                _effect.send(QuickSaveAction.ShowToast("Selected App Not Installed"))
                            } else if (!hasSafPermission) {
                                _effect.send(QuickSaveAction.ShowToast("Selected App Does Not Have SAF Permission"))
                            }
                        }

                        var updatedState = it

                        if (enabled != null) {
                            updatedState = updatedState.copy(isQuickSaveEnabled = enabled)
                        }

                        if (title != null) {
                            updatedState = updatedState.copy(selectedTitle = title)
                        }

                        if (fileType != null) {
                            updatedState = if (fileType.second) {
                                updatedState.copy(selectedMediaTypes = updatedState.selectedMediaTypes + fileType.first)
                            } else {
                                updatedState.copy(selectedMediaTypes = updatedState.selectedMediaTypes - fileType.first)
                            }
                        }

                        updatedState
                    }
                }
            }

            is QuickSaveEvent.RequestPermission -> {
                viewModelScope.launch {
                    _effect.send(QuickSaveAction.RequestNotificationPermission)
                }
            }

            is QuickSaveEvent.ShowNotificationSettingsDialog -> {
                viewModelScope.launch {
                    _effect.send(QuickSaveAction.ShowNotificationPermissionSettingsDialog)
                }
            }

            is QuickSaveEvent.SaveIfPossibleAndBack -> {
                viewModelScope.launch {
                    val title = _uiState.value.selectedTitle
                    val mediaFiles = _uiState.value.selectedMediaTypes
                    val enable = _uiState.value.isQuickSaveEnabled
                    if (enable) {
                        if (mediaFiles.isEmpty()) {
                            _effect.send(QuickSaveAction.ShowToast("Please select at least one media type"))
                            return@launch
                        }
                        if (!appInstallCheckerUseCase.isInstalled(title.packageName)) {
                            _effect.send(QuickSaveAction.ShowToast("Selected App Not Installed"))
                            return@launch
                        } else if (!appInstallCheckerUseCase.hasSafAccessPermission(title.uri)) {
                            _effect.send(QuickSaveAction.ShowToast("Selected App Does Not Have SAF Permission"))
                            return@launch
                        }
                    }
                    saveQuickSaveSettings()
                    _effect.send(QuickSaveAction.GoBack(startForegroundService = enable))
                }
            }

        }
    }

    private suspend fun saveQuickSaveSettings() {
        val title = _uiState.value.selectedTitle
        val mediaFiles = _uiState.value.selectedMediaTypes
        val enabled = _uiState.value.isQuickSaveEnabled

        Log.d("QuickSave", "Saving Settings:")
        Log.d("QuickSave", "Title: $title")
        Log.d("QuickSave", "MediaTypes: $mediaFiles")
        Log.d("QuickSave", "Enabled: $enabled")

        val userPref = QuickSaveUserPref(
            app = title,
            mediaType = mediaFiles.toList(),
            enable = enabled
        )

        val gson = GsonBuilder()
            .registerTypeHierarchyAdapter(Title::class.java, TitleTypeAdapter)
            .create()

        val json = gson.toJson(userPref)

        Log.d("QuickSave", "Generated JSON: $json")

        dataStorePreferenceManager.putPreference(KEY_QUICK_SAVE_USER_PREF, json)

        Log.d("QuickSave", "Saved to DataStore successfully")
    }

}

data class QuickSaveSettingsUiState(
    var isQuickSaveEnabled: Boolean = false,
    var selectedTitle: Title = Title.Whatsapp,
    val selectedMediaTypes: Set<MediaType> = setOf(
        MediaType.IMAGE, MediaType.VIDEO, MediaType.AUDIO
    ),
)

sealed interface QuickSaveEvent {

    data class OnQuickSaveSettingsChanged(
        val title: Title? = null,
        val fileType: Pair<MediaType, Boolean>? = null,
        val isEnabled: Boolean? = null,
    ) : QuickSaveEvent

    data object RequestPermission : QuickSaveEvent
    data object ShowRationale : QuickSaveEvent
    data object SaveIfPossibleAndBack : QuickSaveEvent
    data object ShowNotificationSettingsDialog : QuickSaveEvent

}

sealed interface QuickSaveAction {
    data object ShowNotificationRationale : QuickSaveAction
    data object ShowNotificationPermissionSettingsDialog : QuickSaveAction
    data object RequestNotificationPermission : QuickSaveAction
    data class GoBack(val startForegroundService: Boolean) : QuickSaveAction
    data class ShowToast(val message: String) : QuickSaveAction
}

