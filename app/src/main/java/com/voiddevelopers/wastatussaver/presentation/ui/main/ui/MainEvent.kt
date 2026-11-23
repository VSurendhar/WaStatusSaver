package com.voiddevelopers.wastatussaver.presentation.ui.main.ui

import com.voiddevelopers.wastatussaver.core.analytics.StorageAccessDeniedReason
import com.voiddevelopers.wastatussaver.domain.model.MediaFile
import com.voiddevelopers.wastatussaver.domain.model.MediaType

sealed interface Event {
    data class ChangeTitle(val title: Title) : Event
    data class ChangeTab(val mediaType: MediaType) : Event
    data class ChangeSelectionMode(val mode: SelectionMode) : Event
    data class ChangeShowOnBoardingUiStatus(val status: Boolean) : Event
    data class OnOnboardingNextClick(val page: Int) : Event
    data class OnOnboardingSkipClick(val page: Int) : Event
    data object SafAccessGranted : Event
    data class SafAccessDenied(val reason: StorageAccessDeniedReason) : Event
    data object OnGrantStorageAccessClick : Event
    data object OnDownloadAppClick : Event
    object RefreshUiState : Event
    data object OnRefreshClick : Event
    data class OnDownloadClick(val mediaFile: MediaFile) : Event
    data class OnDrawerOptionClick(val option: DrawerOption) : Event
    data class OnDrawerStateChanged(val isOpen: Boolean) : Event
}

/** Options shown in the navigation drawer. [analyticsName] is the value sent to analytics. */
enum class DrawerOption(val analyticsName: String) {
    HOW_TO_USE("how_to_use"),
    SAVED_STATUS("saved_status"),
    PRIVACY_POLICY("privacy_policy"),
    RATE_APP("rate_app"),
    STAR_ON_GITHUB("star_on_github"),
    REPORT_BUG("report_bug"),
    ABOUT("about"),
}
