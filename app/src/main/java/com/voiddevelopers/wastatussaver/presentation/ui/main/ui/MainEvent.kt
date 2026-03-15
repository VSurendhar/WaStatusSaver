package com.voiddevelopers.wastatussaver.presentation.ui.main.ui

import com.voiddevelopers.wastatussaver.domain.model.MediaFile
import com.voiddevelopers.wastatussaver.domain.model.MediaType

sealed interface Event {
    data class ChangeTitle(val title: Title) : Event
    data class ChangeTab(val mediaType: MediaType) : Event
    data class ChangeSelectionMode(val mode: SelectionMode) : Event
    data class ChangeShowOnBoardingUiStatus(val status: Boolean) : Event
    data class ChangeSAFAccessPermission(val hasSafAccessPermission: Boolean?) : Event
    data class ChangeAppInstalledStatus(val status: Boolean?) : Event
    object RefreshUiState : Event
    data class OnDownloadClick(val mediaFile: MediaFile) : Event
    data object ShowNotificationPermissionDialog : Event
    data object ShowNotificationPermissionSettingsDialog : Event
    data object NotificationPermissionDialogDismiss : Event
    data object NotificationSettingsDialogDismiss : Event
}
