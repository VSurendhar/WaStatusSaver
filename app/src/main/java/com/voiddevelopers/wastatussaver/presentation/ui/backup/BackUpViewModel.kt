package com.voiddevelopers.wastatussaver.presentation.ui.backup

import androidx.lifecycle.ViewModel
import com.voiddevelopers.wastatussaver.domain.model.BackupInterval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class BackUpViewModel : ViewModel() {

    private val _state = MutableStateFlow(BackupSettingsState())
    val state: StateFlow<BackupSettingsState> = _state

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
                    val updatedMediaTypes = if (currentState.selectedMediaTypes.contains(event.mediaType)) {
                        currentState.selectedMediaTypes - event.mediaType
                    } else {
                        currentState.selectedMediaTypes + event.mediaType
                    }
                    currentState.copy(selectedMediaTypes = updatedMediaTypes)
                }
            }

            is BackupEvent.SaveClicked -> {

            }

        }
    }

}