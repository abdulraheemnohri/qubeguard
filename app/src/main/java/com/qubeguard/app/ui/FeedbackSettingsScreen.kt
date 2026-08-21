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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.ui.theme.QubeGuardTheme

/**
 * Feedback Settings Screen for opting in/out of telemetry and managing feedback.
 */
@Composable
fun FeedbackSettingsScreen() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val isTelemetryEnabled by viewModel.isTelemetryEnabled.observeAsState(false)
    val falsePositiveCount by viewModel.falsePositiveCount.observeAsState(0)
    val allowAlwaysCount by viewModel.allowAlwaysCount.observeAsState(0)

    LaunchedEffect(Unit) {
        viewModel.loadFeedbackStats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Feedback Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Help improve QubeGuard by sharing anonymous feedback.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Telemetry Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "Enable Telemetry",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isTelemetryEnabled,
                onCheckedChange = { viewModel.setTelemetryEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Telemetry helps us improve model precision by collecting anonymous feedback on false positives and negatives.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback Stats
        Text(
            text = "Feedback Statistics",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "False Positives Reported: $falsePositiveCount",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Allow Always Decisions: $allowAlwaysCount",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FeedbackSettingsScreenPreview() {
    QubeGuardTheme {
        Surface {
            FeedbackSettingsScreen()
        }
    }
}
