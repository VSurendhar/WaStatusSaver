package com.voiddevelopers.wastatussaver.presentation.ui.backup

import com.voiddevelopers.wastatussaver.domain.model.BackupInterval
import com.voiddevelopers.wastatussaver.domain.model.MediaType

data class BackupSettingsState(
    val isBackupOn: Boolean = false,
    val selectedGoogleAccount: String? = null,
    val backupInterval: BackupInterval = BackupInterval.DAILY,
    val canUseCellular: Boolean = false,
    val selectedMediaTypes: Set<MediaType> = setOf(MediaType.IMAGE, MediaType.VIDEO, MediaType.AUDIO)
)