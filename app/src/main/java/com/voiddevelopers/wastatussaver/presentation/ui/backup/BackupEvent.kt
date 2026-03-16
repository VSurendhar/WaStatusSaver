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

    data object SaveClicked : BackupEvent

}