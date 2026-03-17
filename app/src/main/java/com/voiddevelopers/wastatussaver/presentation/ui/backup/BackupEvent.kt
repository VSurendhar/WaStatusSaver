package com.voiddevelopers.wastatussaver.presentation.ui.backup

import com.voiddevelopers.wastatussaver.domain.model.BackupInterval
import com.voiddevelopers.wastatussaver.domain.model.MediaType

sealed interface BackupEvent {

    data class ToggleBackup(
        val isEnabled: Boolean,
    ) : BackupEvent

    data class SelectAccount(
        val account: String?,
    ) : BackupEvent

    data class ChangeInterval(
        val interval: BackupInterval,
    ) : BackupEvent

    data class SetCellularUsage(
        val canUseCellular: Boolean,
    ) : BackupEvent

    data class ToggleMediaType(
        val mediaType: MediaType,
    ) : BackupEvent

    data class ShowAccountSwitchWarningDialog(val show: Boolean) : BackupEvent

    data object ShowRationale : BackupEvent

    data object ShowNotificationSettingsDialog : BackupEvent
    data object RequestPermission : BackupEvent
    data object StartWork : BackupEvent
    data class SetWorkType(val workType: WorkType) : BackupEvent

}

sealed interface BackupAction {
    data object GoBack : BackupAction

    data class ShowToast(val message: String) : BackupAction
    data object ShowSavedMessage : BackupAction

    data object ShowNotificationRationale : BackupAction

    data object ShowNotificationPermissionSettingsDialog : BackupAction

    data object RequestNotificationPermission : BackupAction
}
