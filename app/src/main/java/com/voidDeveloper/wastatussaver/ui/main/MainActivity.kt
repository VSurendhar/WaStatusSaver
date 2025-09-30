package com.voidDeveloper.wastatussaver.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.voidDeveloper.wastatussaver.navigation.AppNavHost
import com.voidDeveloper.wastatussaver.navigation.Screens
import com.voidDeveloper.wastatussaver.ui.theme.WaStatusSaverTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
