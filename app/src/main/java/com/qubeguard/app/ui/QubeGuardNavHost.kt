package com.qubeguard.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * Sets up Jetpack Compose Navigation with Bottom Navigation Bar for QubeGuard.
 */
@Composable
fun QubeGuardNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem("Home", NavGraph.MAIN, Icons.Default.Home),
        BottomNavItem("Browser", NavGraph.BROWSER, Icons.Default.Info),
        BottomNavItem("Blocklists", NavGraph.BLOCKLIST_SETTINGS, Icons.AutoMirrored.Filled.List),
        BottomNavItem("Qubes", NavGraph.QUBE_MANAGEMENT, Icons.Default.Lock),
        BottomNavItem("Settings", NavGraph.SETTINGS, Icons.Default.Settings)
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavGraph.MAIN,
            modifier = Modifier.padding(innerPadding)
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
}

private data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
