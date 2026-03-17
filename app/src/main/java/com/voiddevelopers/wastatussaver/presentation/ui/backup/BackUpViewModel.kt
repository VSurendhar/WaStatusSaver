package com.voiddevelopers.wastatussaver.presentation.ui.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.voiddevelopers.wastatussaver.data.model.BackUpUserPref
import com.voiddevelopers.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManager
import com.voiddevelopers.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManagerImpl.DataStoreKeys.KEY_BACK_UP_USER_PREF
import com.voiddevelopers.wastatussaver.data.utils.extentions.isInternetAvailable
import com.voiddevelopers.wastatussaver.domain.model.BackupInterval
import com.voiddevelopers.wastatussaver.domain.model.MediaType
import com.voiddevelopers.wastatussaver.presentation.ui.backup.BackupAction.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackUpViewModel @Inject constructor(
    val backupWorkerScheduler: BackupWorkerScheduler,
    val dataStorePreferenceManager: DataStorePreferenceManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupSettingsState())
    val state: StateFlow<BackupSettingsState> = _state

    private val _event = MutableSharedFlow<BackupAction>()
    val event: SharedFlow<BackupAction> = _event

    private var workType: WorkType? = null

    init {
        viewModelScope.launch {
            val backUpConfig = getConfiguration()
            _state.update {
                it.copy(
                    isBackupOn = backUpConfig?.allowBackUp ?: false,
                    backupInterval = backUpConfig?.interval ?: BackupInterval.DAILY,
                    canUseCellular = backUpConfig?.canUseCellular ?: false,
                    selectedMediaTypes = (backUpConfig?.selectedMediaTypes ?: listOf(
                        MediaType.IMAGE, MediaType.VIDEO, MediaType.AUDIO
                    )).toSet(),
                )
            }
        }
    }

    fun onEvent(event: BackupEvent) {
        when (event) {

            is BackupEvent.ToggleBackup -> {
                _state.update { it.copy(isBackupOn = event.isEnabled) }
            }

            is BackupEvent.SelectAccount -> {
                _state.update { it.copy(selectedGoogleAccount = event.account) }
            }

            is BackupEvent.ChangeInterval -> {
                _state.update { it.copy(backupInterval = event.interval) }
            }

            is BackupEvent.SetCellularUsage -> {
                _state.update { it.copy(canUseCellular = event.canUseCellular) }
            }

            is BackupEvent.ToggleMediaType -> {
                _state.update { currentState ->
                    val updatedMediaTypes =
                        if (currentState.selectedMediaTypes.contains(event.mediaType)) {
                            currentState.selectedMediaTypes - event.mediaType
                        } else {
                            currentState.selectedMediaTypes + event.mediaType
                        }
                    currentState.copy(selectedMediaTypes = updatedMediaTypes)
                }
            }

            is BackupEvent.ShowAccountSwitchWarningDialog -> {
                _state.update { it.copy(showAccountSwitchWarningDialog = event.show) }
            }

            is BackupEvent.ShowRationale -> {
                viewModelScope.launch {
                    _event.emit(BackupAction.ShowNotificationRationale)
                }
            }

            is BackupEvent.ShowNotificationSettingsDialog -> {
                viewModelScope.launch {
                    _event.emit(BackupAction.ShowNotificationPermissionSettingsDialog)
                }
            }

            is BackupEvent.RequestPermission -> {
                viewModelScope.launch {
                    _event.emit(BackupAction.RequestNotificationPermission)
                }
            }

            is BackupEvent.StartWork -> {
                viewModelScope.launch {

                    if (_state.value.selectedGoogleAccount == null) {
                        _event.emit(ShowToast("Google Account Not Set"))
                        return@launch
                    }

                    backupWorkerScheduler.cancelAllWorkers(context)
                    saveConfiguration(
                        BackUpUserPref(
                            interval = _state.value.backupInterval,
                            selectedMediaTypes = _state.value.selectedMediaTypes.toList(),
                            allowBackUp = _state.value.isBackupOn,
                            canUseCellular = _state.value.canUseCellular
                        )
                    )
                    if (workType == WorkType.EXPEDITED) {
                        backupWorkerScheduler.setExpeditedWorker(
                            context = context,
                            selectedMediaTypes = _state.value.selectedMediaTypes.toList()
                        )
                    } else {
                        if (_state.value.isBackupOn) {
                            backupWorkerScheduler.setPeriodicWorker(
                                context,
                                _state.value.backupInterval,
                                _state.value.selectedMediaTypes.toList(),
                                _state.value.canUseCellular
                            )
                        }
                    }
                    _event.emit(BackupAction.ShowSavedMessage)
                }
            }

            is BackupEvent.SetWorkType -> {
                this.workType = event.workType
            }

        }
    }

    suspend fun saveConfiguration(backUpUserPref: BackUpUserPref) {
        dataStorePreferenceManager.putPreference(
            KEY_BACK_UP_USER_PREF, value = Gson().toJson(backUpUserPref)
        )
    }

    suspend fun getConfiguration(): BackUpUserPref? {
        val str = dataStorePreferenceManager.getPreference(KEY_BACK_UP_USER_PREF, "").first()
        return try {
            Gson().fromJson(str, BackUpUserPref::class.java)
        } catch (e: Exception) {
            null
        }
    }

}

enum class WorkType {
    EXPEDITED,
    PERIODIC
}