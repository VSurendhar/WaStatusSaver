package com.voidDeveloper.wastatussaver.ui.main

sealed interface Event {
    data class ChangeTitle(val title: Title) : Event
    data class ChangeTab(val fileType: FileType) : Event
    data class ChangeSelectionMode(val mode: SelectionMode) : Event
    data class ChangeShowOnBoardingUiStatus(val status: Boolean) : Event
    data class ChangeSAFAccessPermission(val hasSafAccessPermission: Boolean?) : Event
    data class ChangeAppInstalledStatus(val status: Boolean?) : Event
}
