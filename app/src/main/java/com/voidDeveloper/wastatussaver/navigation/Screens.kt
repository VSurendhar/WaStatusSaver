package com.voidDeveloper.wastatussaver.navigation

sealed class Screens(val route: String) {

    object Main : Screens("screen.main")
    object SavedStatus : Screens("screen.saved.status")
    object WebView : Screens("screen.web.view")
    object AutoSaveSettings : Screens("screen.autosave.settings")

}