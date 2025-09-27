package com.voidDeveloper.wastatussaver.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.voidDeveloper.wastatussaver.ui.main.MainScreen
import com.voidDeveloper.wastatussaver.ui.main.MainViewModel
import com.voidDeveloper.wastatussaver.ui.messageUnSaved.MessageUnSavedScreen
import com.voidDeveloper.wastatussaver.ui.savedStatus.SavedStatusScreen
import com.voidDeveloper.wastatussaver.ui.webView.WebViewScreen

@Composable
fun AppNavHost(navController: NavHostController, startDestination: String) {

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screens.Main.route) {
            val viewModel = hiltViewModel<MainViewModel>()
            MainScreen(uiState = viewModel.uiState, onEvent = viewModel::onEvent)
        }
        composable(Screens.SavedStatus.route) {
            SavedStatusScreen()
        }
        composable(Screens.WebView.route) {
            WebViewScreen()
        }
        composable(Screens.MessageUnSaved.route) {
            MessageUnSavedScreen()
        }
    }

}