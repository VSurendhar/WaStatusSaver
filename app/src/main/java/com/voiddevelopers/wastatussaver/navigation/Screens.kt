package com.voiddevelopers.wastatussaver.navigation

sealed class Screens(val route: String) {

    object Main : Screens("screen.main")
    object SavedStatus : Screens("screen.saved.status")
    object WebView : Screens("screen.web.view")
    object QuickSaveSettings : Screens("screen.quicksave.settings")
    object BackUp : Screens("screen.backup")

}