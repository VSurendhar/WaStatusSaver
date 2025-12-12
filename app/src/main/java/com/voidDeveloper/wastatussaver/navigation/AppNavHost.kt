package com.voidDeveloper.wastatussaver.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.MainScreen
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.MainViewModel
import com.voidDeveloper.wastatussaver.presentation.ui.savedStatus.SavedStatusScreen
import com.voidDeveloper.wastatussaver.presentation.ui.webView.WebViewScreen
import java.net.URLDecoder

@Composable
fun AppNavHost(navController: NavHostController, startDestination: String) {

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screens.Main.route) {
            val viewModel = hiltViewModel<MainViewModel>()
            MainScreen(
                uiState = viewModel.uiState,
                onEvent = viewModel::onEvent,
                navigate = { route: String ->
                    navController.navigate(route)
                })
        }
        composable(Screens.SavedStatus.route) {
            SavedStatusScreen()
        }
        composable(
            route = "${Screens.WebView.route}/{url}",
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url"), "UTF-8")
            WebViewScreen(url = url)
        }
    }

}