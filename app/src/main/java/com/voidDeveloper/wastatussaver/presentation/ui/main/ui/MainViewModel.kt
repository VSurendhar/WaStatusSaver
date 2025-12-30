package com.voidDeveloper.wastatussaver.presentation.ui.main.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.KEY_PREFERRED_TITLE
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.KEY_SHOULD_SHOW_ONBOARDING_UI
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager.DataStoreKeys.USER_PREF_AUTO_SAVE
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStorePreferenceManager
import com.voidDeveloper.wastatussaver.data.utils.Constants.DEFAULT_AUTO_SAVE_INTERVAL
import com.voidDeveloper.wastatussaver.data.utils.getMillisFromNow
import com.voidDeveloper.wastatussaver.data.utils.helpers.ScheduleAutoSave
import com.voidDeveloper.wastatussaver.domain.model.AutoSave
import com.voidDeveloper.wastatussaver.domain.model.MediaFile
import com.voidDeveloper.wastatussaver.domain.usecases.AppInstallCheckerUseCase
import com.voidDeveloper.wastatussaver.domain.usecases.StatusesManagerUseCase
import com.voidDeveloper.wastatussaver.domain.usecases.TelegramLogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStorePreferenceManager: DataStorePreferenceManager,
    private val statusesManagerUseCase: StatusesManagerUseCase,
    private val appInstallChecker: AppInstallCheckerUseCase,
    private val telegramLogUseCase: TelegramLogUseCase,
    private val scheduleAutoSave: ScheduleAutoSave,
) : ViewModel() {

    private val _uiState: MutableStateFlow<UiState?> = MutableStateFlow(
        null
    )

    val uiState = _uiState.asStateFlow()

    private val _toastInfoChannel = Channel<String?>()
    val toastInfoChannel = _toastInfoChannel.receiveAsFlow()


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
                _uiState.update { it?.copy(currentFileType = event.fileType) }
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
                _uiState.update {
                    it?.copy(
                        onGoingDownload = (it.onGoingDownload + event.mediaFile).distinct()
                            .toMutableList()
                    )
                }
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

            is Event.SaveAutoSaveData -> {
                viewModelScope.launch {
                    try {
                        val interval = getAutoSaveInterval()
                        saveAutoSaveData(enable = event.enable)
                        if (event.enable) {
                            if (interval != event.interval) {
                                saveAutoSaveData(interval = event.interval)
                                val triggerAtMillis = getMillisFromNow(event.interval)
                                scheduleAutoSave.scheduleAutoSaveWorkAlarm(triggerAtMillis)
                            } else {
                                _toastInfoChannel.send("Interval already set")
                            }
                        } else {
                            scheduleAutoSave.cancelAllAlarm()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                _uiState.update {
                    it?.copy(
                        showAutoSaveDialog = false,
                        autoSaveEnabled = event.enable,
                    )
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
        val userPrefAutoSave = dataStorePreferenceManager.getPreference(
            USER_PREF_AUTO_SAVE, defaultValue = ""
        ).first()
        val gson = Gson()
        val ufAutoSave = gson.fromJson(userPrefAutoSave, AutoSave::class.java)
        return ufAutoSave?.interval
    }

    private suspend fun saveAutoSaveData(interval: Int? = null, enable: Boolean? = null) {
        val gson = Gson()
        val userPrefAutoSave = dataStorePreferenceManager.getPreference(
            USER_PREF_AUTO_SAVE, defaultValue = ""
        ).first()
        val ufAutoSave: AutoSave =
            gson.fromJson(userPrefAutoSave, AutoSave::class.java) ?: AutoSave()
        if (interval != null) ufAutoSave.interval = interval
        if (enable != null) {
            ufAutoSave.isAutoSaveEnable = enable
            if (!enable) ufAutoSave.interval = null
        }
        val userPrefNewAutoSave = gson.toJson(ufAutoSave)
        dataStorePreferenceManager.putPreference(USER_PREF_AUTO_SAVE, userPrefNewAutoSave)
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
            statusesManagerUseCase.saveMediaFile(mediaFile, onSaveCompleted = {
                mediaFile.isDownloaded = true
                _uiState.update {
                    it?.copy(
                        onGoingDownload = (it.onGoingDownload - mediaFile).distinct()
                            .toMutableList()
                    )
                }
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
        val userPrefAutoSave = dataStorePreferenceManager.getPreference(
            USER_PREF_AUTO_SAVE, defaultValue = ""
        ).first()
        val gson = Gson()
        val ufAutoSave = gson.fromJson(userPrefAutoSave, AutoSave::class.java)
        return ufAutoSave?.isAutoSaveEnable ?: false
    }

}

