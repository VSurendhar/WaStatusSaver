package com.voidDeveloper.wastatussaver.ui.main

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.KEY_PREFERRED_TITLE
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.KEY_SHOULD_SHOW_ONBOARDING_UI
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStorePreferenceManager
import com.voidDeveloper.wastatussaver.data.utils.Constants.TAG
import com.voidDeveloper.wastatussaver.data.utils.Constants.WHATSAPP
import com.voidDeveloper.wastatussaver.data.utils.Constants.WHATSAPP_BUSINESS
import com.voidDeveloper.wastatussaver.ui.main.componenets.AppInstallChecker
import com.voidDeveloper.wastatussaver.ui.main.componenets.StatusesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStorePreferenceManager: DataStorePreferenceManager,
    private val statusesManager: StatusesManager,
    private val appInstallChecker: AppInstallChecker,
) : ViewModel() {

    private val _uiState: MutableStateFlow<UiState?> = MutableStateFlow(
        null
    )

    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val preferredTitle = getPreferredTitle()
            val shouldShowOnBoardingUi = shouldShowOnBoardingUi()
            val appInstalled = getAppInstalledStatus(preferredTitle)
            val hasSafAccessPermission = hasSafAccessPermission(appInstalled, preferredTitle)
            _uiState.value = UiState(
                title = preferredTitle,
                appInstalled = appInstalled,
                shouldShowOnBoardingUi = shouldShowOnBoardingUi,
                hasSafAccessPermission = hasSafAccessPermission,
                selectionMode = SelectionMode.SINGLE_SELECT,
                currentFileType = FileType.IMAGES,
                mediaFiles = getFiles(preferredTitle)
            )
        }
    }


    private fun hasSafAccessPermission(appInstalled: Boolean, preferredTitle: Title): Boolean {
        return (if (appInstalled) statusesManager.hasPermission(preferredTitle.uri) else null) == true
    }

    private fun getAppInstalledStatus(preferredTitle: Title): Boolean {
        return appInstallChecker.isInstalled(preferredTitle.packageName)
    }

    private suspend fun shouldShowOnBoardingUi(): Boolean {
        return dataStorePreferenceManager.getPreference(
            KEY_SHOULD_SHOW_ONBOARDING_UI, defaultValue = true
        ).first()
    }

    suspend fun getPreferredTitle(): Title {
        val preferredId = dataStorePreferenceManager.getPreference(
            KEY_PREFERRED_TITLE, defaultValue = WHATSAPP
        ).first()

        return when (preferredId) {
            WHATSAPP_BUSINESS -> Title.WhatsappBusiness
            else -> Title.Whatsapp
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.ChangeTab -> {
                _uiState.update { it?.copy(currentFileType = event.fileType) }
            }

            is Event.ChangeTitle -> {
                viewModelScope.launch {
                    val appInstalled by lazy { appInstallChecker.isInstalled(event.title.packageName) }
                    val hasSafPermission by lazy { statusesManager.hasPermission(event.title.uri) }
                    if (appInstalled && hasSafPermission) {
                        setPreferredTitle(event.title)
                        _uiState.update {
                            it?.copy(
                                title = event.title,
                                hasSafAccessPermission = true,
                                appInstalled = true,
                                mediaFiles = getFiles(event.title)
                            )
                        }
                    } else if (!appInstalled) {
                        _uiState.update {
                            it?.copy(
                                title = event.title,
                                hasSafAccessPermission = false,
                                mediaFiles = emptyList(),
                                appInstalled = false
                            )
                        }
                    } else {
                        _uiState.update {
                            it?.copy(
                                title = event.title,
                                hasSafAccessPermission = false,
                                mediaFiles = emptyList(),
                                appInstalled = false
                            )
                        }
                    }
                }
            }

            is Event.ChangeSelectionMode -> {
                _uiState.update { it?.copy(selectionMode = event.mode) }
            }

            is Event.ChangeSAFAccessPermission -> {
                _uiState.update { it?.copy(hasSafAccessPermission = event.hasSafAccessPermission) }
            }

            is Event.ChangeShowOnBoardingUiStatus -> {
                viewModelScope.launch {
                    _uiState.update { it?.copy(shouldShowOnBoardingUi = event.status) }
                    dataStorePreferenceManager.putPreference(
                        KEY_SHOULD_SHOW_ONBOARDING_UI, event.status
                    )
                }
            }

            is Event.FetchStatusesMedia -> {
                viewModelScope.launch {
                    _uiState.update {
                        it?.copy(mediaFiles = getFiles(event.title))
                    }
                }
            }
        }
    }

    private suspend fun setPreferredTitle(title: Title) {
        dataStorePreferenceManager.putPreference(KEY_PREFERRED_TITLE, Gson().toJson(title))
    }

    private fun getFiles(title: Title): List<File> {
        if (_uiState.value?.hasSafAccessPermission == true) {
            val files = statusesManager.getFiles(title.uri)
            val filteredFiles: List<File> = files.filter { true }
            return filteredFiles
        } else {
            return emptyList()
        }
    }

}

open class File(
    val id: String = UUID.randomUUID().toString(),
    val isDownloaded: Boolean = true,
    val uri: Uri? = "".toUri(),
    val fileType: FileType,
)

