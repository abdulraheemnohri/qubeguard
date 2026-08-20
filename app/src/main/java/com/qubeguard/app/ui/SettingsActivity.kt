package com.qubeguard.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.ui.theme.QubeGuardTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Settings Activity for QubeGuard.
 * Displays settings for blocklists, Qubes, ML, and feedback.
 */
@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QubeGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen()
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = hiltViewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Blocklist Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        // Blocklist settings content would go here
        // Example: Toggle for enabling/disabling blocklist sources

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Qube Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        // Qube settings content would go here
        // Example: List of Qubes with options to edit/delete

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ML Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        // ML settings content would go here
        // Example: Toggle for enabling/disabling ML classifier

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Feedback Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        // Feedback settings content would go here
        // Example: Toggle for opt-in/out of telemetry
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    QubeGuardTheme {
        SettingsScreen()
    }
}
