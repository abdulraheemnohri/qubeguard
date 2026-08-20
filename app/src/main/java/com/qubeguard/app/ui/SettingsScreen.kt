package com.qubeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.qubeguard.app.ui.theme.QubeGuardTheme

/**
 * Updated Settings Screen with navigation to sub-settings.
 */
@Composable
fun SettingsScreen() {
    val navController = rememberNavController()

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

        Button(
            onClick = { /* Navigate to Blocklist Settings */ }
        ) {
            Text("Blocklist Settings")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { /* Navigate to Qube Management */ }
        ) {
            Text("Qube Management")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { /* Navigate to ML Settings */ }
        ) {
            Text("ML Settings")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { /* Navigate to Feedback Settings */ }
        ) {
            Text("Feedback Settings")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    QubeGuardTheme {
        Surface {
            SettingsScreen()
        }
    }
}
