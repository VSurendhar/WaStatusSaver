package com.voidDeveloper.wastatussaver.presentation.ui.main.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidDeveloper.wastatussaver.data.datastore.proto.MediaType
import com.voidDeveloper.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManager
import com.voidDeveloper.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManagerImpl.DataStoreKeys.KEY_PREFERRED_TITLE
import com.voidDeveloper.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManagerImpl.DataStoreKeys.KEY_SHOULD_SHOW_ONBOARDING_UI
import com.voidDeveloper.wastatussaver.data.protodatastoremanager.AutoSaveProtoDataStoreManager
import com.voidDeveloper.wastatussaver.data.utils.Constants.DEFAULT_AUTO_SAVE_INTERVAL
import com.voidDeveloper.wastatussaver.data.utils.extentions.getInterval
import com.voidDeveloper.wastatussaver.domain.model.MediaFile
import com.voidDeveloper.wastatussaver.domain.usecases.AppInstallCheckerUseCase
import com.voidDeveloper.wastatussaver.domain.usecases.SavedMediaHandlingUserCase
import com.voidDeveloper.wastatussaver.domain.usecases.StatusesManagerUseCase
import com.voidDeveloper.wastatussaver.domain.usecases.TelegramLogUseCase
import com.voidDeveloper.wastatussaver.presentation.ui.player.ui.videoAudioPlayerRoot.DownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStorePreferenceManager: DataStorePreferenceManager,
    private val autoSaveProtoDataStoreManager: AutoSaveProtoDataStoreManager,
    private val statusesManagerUseCase: StatusesManagerUseCase,
    private val appInstallChecker: AppInstallCheckerUseCase,
    private val telegramLogUseCase: TelegramLogUseCase,
    private val statusMediaDownloadHandler : SavedMediaHandlingUserCase
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
                currentMediaType = MediaType.IMAGE,
                mediaFiles = if (!shouldShowOnBoardingUi && appInstalled && hasSafAccessPermission) {
                    getFiles(preferredTitle)
                } else {
                    emptyList()
                },
                lastRefreshTimestamp = System.currentTimeMillis(),
                autoSaveEnabled = isAutoSaveEnable()
            )
        }

    }


    private fun hasSafAccessPermission(appInstalled: Boolean, preferredTitle: Title): Boolean {
        return (if (appInstalled) statusesManagerUseCase.hasPermission(preferredTitle.uri) else null) == true
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

        val preferredId = try {
            dataStorePreferenceManager.getPreference(
                KEY_PREFERRED_TITLE, defaultValue = Title.Whatsapp.packageName
            ).first()
        } catch (e: Exception) {
            e.printStackTrace()
            Title.Whatsapp.packageName
        }

        val title = when (preferredId) {
            Title.WhatsappBusiness.packageName -> {
                Title.WhatsappBusiness
            }

            else -> {
                Title.Whatsapp
            }
        }

        return title
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.ChangeTab -> {
                _uiState.update { it?.copy(currentMediaType = event.mediaType) }
            }

            is Event.ChangeTitle -> {
                viewModelScope.launch {
                    val appInstalled by lazy { appInstallChecker.isInstalled(event.title.packageName) }
                    val hasSafPermission by lazy { statusesManagerUseCase.hasPermission(event.title.uri) }
                    setPreferredTitle(event.title)
                    if (appInstalled && hasSafPermission) {
                        val files = getFiles(event.title)
                        _uiState.update {
                            it?.copy(
                                title = event.title,
                                hasSafAccessPermission = true,
                                appInstalled = true,
                                mediaFiles = files
                            )
                        }
                    } else if (!appInstalled) {
                        _uiState.update {
                            it?.copy(
                                title = event.title,
                                hasSafAccessPermission = null,
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
                                appInstalled = true
                            )
                        }
                    }
                }
            }

            is Event.ChangeSelectionMode -> {
                _uiState.update { it?.copy(selectionMode = event.mode) }
            }

            is Event.ChangeSAFAccessPermission -> {
                _uiState.update {
                    if (event.hasSafAccessPermission == true) {
                        val files = getFiles(uiState.value?.title!!)
                        it?.copy(mediaFiles = files)
                    }
                    it?.copy(hasSafAccessPermission = event.hasSafAccessPermission)
                }
            }

            is Event.ChangeShowOnBoardingUiStatus -> {
                viewModelScope.launch {
                    _uiState.update { it?.copy(shouldShowOnBoardingUi = event.status) }
                    dataStorePreferenceManager.putPreference(
                        KEY_SHOULD_SHOW_ONBOARDING_UI, event.status
                    )
                }
            }

            is Event.ChangeAppInstalledStatus -> {
                _uiState.update {
                    it?.copy(
                        appInstalled = event.status, hasSafAccessPermission = null
                    )
                }
            }

            is Event.RefreshUiState -> {
                refreshUiState()
            }

            is Event.OnDownloadClick -> {
                saveMediaFile(event.mediaFile)
            }

            is Event.ShowAutoSaveDialog -> {
                viewModelScope.launch {
                    _uiState.update {
                        it?.copy(
                            showAutoSaveDialog = true,
                            savedAutoSaveInterval = getAutoSaveInterval()
                                ?: DEFAULT_AUTO_SAVE_INTERVAL
                        )
                    }
                }
            }

            is Event.ShowNotificationPermissionDialog -> {
                _uiState.update {
                    it?.copy(
                        showNotificationPermissionDialog = true,
                    )
                }
            }

            is Event.ShowNotificationPermissionSettingsDialog -> {
                _uiState.update {
                    it?.copy(
                        showNotificationPermissionSettingsDialog = true,
                    )
                }
            }

            is Event.NotificationPermissionDialogDismiss -> {
                _uiState.update {
                    it?.copy(
                        showNotificationPermissionDialog = false,
                    )
                }
            }

            is Event.NotificationSettingsDialogDismiss -> {
                _uiState.update {
                    it?.copy(
                        showNotificationPermissionSettingsDialog = false
                    )
                }
            }

            is Event.AutoSaveDialogDismiss -> {
                _uiState.update {
                    it?.copy(
                        showAutoSaveDialog = false,
                    )
                }
            }

            is Event.SendLogsTelegram -> {
                viewModelScope.launch(Dispatchers.IO) {
                    telegramLogUseCase.sendLogs(event.logs)
                }
            }

        }

    }

    private suspend fun getAutoSaveInterval(): Int? {
        val autoSaveUserPref = autoSaveProtoDataStoreManager.readAutoSaveUserPref()
        val interval = autoSaveUserPref.getInterval()
        return interval
    }

    private fun refreshUiState() {
        viewModelScope.launch {
            val preferredTitle = getPreferredTitle()
            val shouldShowOnBoardingUi = shouldShowOnBoardingUi()
            val appInstalled = getAppInstalledStatus(preferredTitle)
            val hasSafAccessPermission = hasSafAccessPermission(appInstalled, preferredTitle)
            _uiState.update { current ->
                val updated = current?.copy(
                    shouldShowOnBoardingUi = shouldShowOnBoardingUi,
                    hasSafAccessPermission = hasSafAccessPermission,
                    appInstalled = appInstalled,
                    lastRefreshTimestamp = System.currentTimeMillis(),
                    mediaFiles = if (hasSafAccessPermission && !shouldShowOnBoardingUi && appInstalled && current.title != null) {
                        getFiles(current.title).toList()
                    } else {
                        current.mediaFiles
                    }
                )

                updated ?: current
            }
        }
    }


    fun saveMediaFile(mediaFile: MediaFile) {
        viewModelScope.launch {
            mediaFile.downloadState = DownloadState.DOWNLOADING
            statusMediaDownloadHandler.saveMediaFile(mediaFile, onSaveCompleted = {
                addDownloadedFileToCache(mediaFile)
                mediaFile.downloadState = DownloadState.DOWNLOADED
            })
        }
    }

    private suspend fun setPreferredTitle(title: Title) {
        dataStorePreferenceManager.putPreference(KEY_PREFERRED_TITLE, title.packageName)
    }

    private fun getFiles(title: Title): List<MediaFile> {
        val files = statusesManagerUseCase.getFiles(title.uri)
        return files
    }

    private suspend fun isAutoSaveEnable(): Boolean {
        val autoSaveUserPref = autoSaveProtoDataStoreManager.readAutoSaveUserPref()
        val enable = autoSaveUserPref.enable
        return enable
    }

    private fun addDownloadedFileToCache(mediaFile: MediaFile) {
        statusesManagerUseCase.addDownloadedFileToCache(mediaFile)
    }

}

