package com.qubeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.ui.theme.QubeGuardTheme

/**
 * ML Settings Screen for enabling/disabling the ML classifier and adjusting thresholds.
 */
@Composable
fun MlSettingsScreen() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val isMlEnabled by viewModel.isMlEnabled.observeAsState(true)
    val isModelLoaded by viewModel.isModelLoaded()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ML Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Configure the ML-based blocking engine.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ML Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "Enable ML Classifier",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isMlEnabled,
                onCheckedChange = { viewModel.setMlEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isModelLoaded) "Model Status: Loaded" else "Model Status: Not Loaded",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Threshold Settings (Placeholder)
        Text(
            text = "Threshold Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Adjust the confidence thresholds for each category.",
            style = MaterialTheme.typography.bodyMedium
        )

        // Threshold sliders would go here
    }
}

@Preview(showBackground = true)
@Composable
fun MlSettingsScreenPreview() {
    QubeGuardTheme {
        Surface {
            MlSettingsScreen()
        }
    }
}
