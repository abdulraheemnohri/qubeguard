package com.qubeguard.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val isMlEnabled by viewModel.isMlEnabled.observeAsState(true)
    val isHuggingFaceEnabled by viewModel.isHuggingFaceEnabled.observeAsState(false)
    var hfToken by remember { mutableStateOf("") }

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

        // ML Settings
        Text(
            text = "ML Classifier Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Enable/Disable ML
        SwitchSetting(
            title = "Enable ML Classifier",
            checked = isMlEnabled,
            onCheckedChange = { viewModel.setMlEnabled(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Hugging Face Toggle
        SwitchSetting(
            title = "Use Hugging Face API",
            subtitle = "Uses r3ddkahili/final-complete-malicious-url-model (requires internet)",
            checked = isHuggingFaceEnabled,
            onCheckedChange = { 
                viewModel.setHuggingFaceEnabled(it)
                if (it) {
                    viewModel.disableLocalModel()
                } else {
                    viewModel.enableLocalModel()
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Hugging Face Token
        if (isHuggingFaceEnabled) {
            TextField(
                value = hfToken,
                onValueChange = { hfToken = it },
                label = { Text("Hugging Face API Token (Optional)") },
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Button(
                onClick = { 
                    viewModel.setHuggingFaceToken(hfToken)
                },
                enabled = hfToken.isNotBlank()
            ) {
                Text("Save Token")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Get your token from: https://huggingface.co/settings/tokens",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Model Status",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (viewModel.isModelLoaded()) "Model: Loaded" else "Model: Not Loaded",
            style = MaterialTheme.typography.bodyMedium
        )

        if (isHuggingFaceEnabled) {
            Text(
                text = "Using: Hugging Face API (r3ddkahili/final-complete-malicious-url-model)",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                text = "Using: Local TFLite Model",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Blocklist Settings
        Button(
            onClick = { /* Navigate to BlocklistSettingsScreen */ }
        ) {
            Text("Blocklist Settings")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Qube Settings
        Button(
            onClick = { /* Navigate to QubeManagementScreen */ }
        ) {
            Text("Qube Management")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Feedback Settings
        Button(
            onClick = { /* Navigate to FeedbackSettingsScreen */ }
        ) {
            Text("Feedback Settings")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Load Model Button
        Button(
            onClick = { viewModel.loadModel() }
        ) {
            Text("Load/Reload Model")
        }
    }
}

@Composable
fun SwitchSetting(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    QubeGuardTheme {
        SettingsScreen()
    }
}
