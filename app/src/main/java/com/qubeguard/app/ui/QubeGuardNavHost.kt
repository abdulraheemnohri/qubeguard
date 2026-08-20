package com.qubeguard.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Sets up Jetpack Compose Navigation for QubeGuard.
 */
@Composable
fun QubeGuardNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavGraph.MAIN,
        modifier = modifier
    ) {
        composable(NavGraph.MAIN) {
            MainScreen()
        }
        composable(NavGraph.BROWSER) {
            BrowserScreen()
        }
        composable(NavGraph.SETTINGS) {
            SettingsScreen()
        }
        composable(NavGraph.BLOCK_PAGE) {
            BlockPageScreen()
        }
    }
}
