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
            MainScreen(
                onNavigateToBrowser = { navController.navigate(NavGraph.BROWSER) },
                onNavigateToSettings = { navController.navigate(NavGraph.SETTINGS) }
            )
        }
        composable(NavGraph.BROWSER) {
            BrowserScreen()
        }
        composable(NavGraph.SETTINGS) {
            SettingsScreen(
                onNavigateToBlocklists = { navController.navigate(NavGraph.BLOCKLIST_SETTINGS) },
                onNavigateToQubes = { navController.navigate(NavGraph.QUBE_MANAGEMENT) },
                onNavigateToAi = { navController.navigate(NavGraph.ML_SETTINGS) },
                onNavigateToFeedback = { navController.navigate(NavGraph.FEEDBACK_SETTINGS) }
            )
        }
        composable(NavGraph.BLOCK_PAGE) {
            BlockPageScreen()
        }
        composable(NavGraph.QUBE_SELECTOR) {
            QubeSelectorScreen(
                onQubeSelected = { navController.popBackStack() },
                onCreateNewQube = { navController.navigate(NavGraph.QUBE_MANAGEMENT) }
            )
        }
        composable(NavGraph.BLOCKLIST_SETTINGS) {
            BlocklistSettingsScreen()
        }
        composable(NavGraph.FEEDBACK_SETTINGS) {
            FeedbackSettingsScreen()
        }
        composable(NavGraph.ML_SETTINGS) {
            MlSettingsScreen()
        }
        composable(NavGraph.QUBE_MANAGEMENT) {
            QubeManagementScreen()
        }
    }
}
