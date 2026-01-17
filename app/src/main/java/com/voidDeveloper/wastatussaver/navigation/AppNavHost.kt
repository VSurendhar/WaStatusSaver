package com.voidDeveloper.wastatussaver.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.voidDeveloper.wastatussaver.data.utils.LifecycleAwareEventCallBacks
import com.voidDeveloper.wastatussaver.presentation.ui.autosavesettings.AutoSaveSettingsScreen
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.Event
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.MainScreen
import com.voidDeveloper.wastatussaver.presentation.ui.main.ui.MainViewModel
import com.voidDeveloper.wastatussaver.presentation.ui.savedStatus.SavedStatusScreen
import com.voidDeveloper.wastatussaver.presentation.ui.webView.WebViewScreen
import java.net.URLDecoder

@Composable
fun AppNavHost(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination, enterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)
        )
    }, exitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)
        )
    }, popEnterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)
        )
    }, popExitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)
        )
    }) {
        composable(Screens.Main.route) {
            val viewModel = hiltViewModel<MainViewModel>()
            LifecycleAwareEventCallBacks(onResume = {
                viewModel.onEvent(Event.RefreshUiState)
            })
            MainScreen(
                uiState = viewModel.uiState,
                onEvent = viewModel::onEvent,
                navigate = { route: String ->
                    navController.navigate(route)
                })
        }
        composable(Screens.SavedStatus.route) {
            SavedStatusScreen(onBack = {
                navController.popBackStack()
            })
        }
        composable(Screens.AutoSaveSettings.route) {
            AutoSaveSettingsScreen(onBackClick = {
                navController.popBackStack()
            })
        }
        composable(
            route = "${Screens.WebView.route}/{url}",
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url"), "UTF-8")
            WebViewScreen(url = url, onBackClick = {
                navController.popBackStack()
            })
        }
    }

}