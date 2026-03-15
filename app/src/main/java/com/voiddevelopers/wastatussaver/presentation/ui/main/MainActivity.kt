package com.voiddevelopers.wastatussaver.presentation.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.voiddevelopers.wastatussaver.data.utils.helpers.WebViewManager
import com.voiddevelopers.wastatussaver.navigation.AppNavHost
import com.voiddevelopers.wastatussaver.navigation.Screens
import com.voiddevelopers.wastatussaver.presentation.theme.WaStatusSaverTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var webViewManager: WebViewManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WaStatusSaverTheme {
                val navController = rememberNavController()
                AppNavHost(navController, Screens.Main.route)
            }
        }
    }
}
