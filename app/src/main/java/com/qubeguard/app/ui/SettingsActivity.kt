package com.qubeguard.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.ui.theme.QubeGuardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QubeGuardTheme {
                Surface(modifier = Modifier.fillMaxSize()) { SettingsScreen() }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onNavigateToBlocklists: (() -> Unit)? = null,
    onNavigateToQubes: (() -> Unit)? = null,
    onNavigateToAi: (() -> Unit)? = null,
    onNavigateToFeedback: (() -> Unit)? = null
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val aiEnabled by viewModel.isMlEnabled.observeAsState(false)
    val autoUpdate by viewModel.isAutoModelUpdateEnabled.observeAsState(false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Privacy controls, protection layers and local AI", style = MaterialTheme.typography.bodyMedium)

        SettingsCard("Protection") {
            SettingSwitch("AI malicious-URL protection", "Optional local Transformer; protection continues without it.", aiEnabled) {
                viewModel.setMlEnabled(it)
            }
            HorizontalDivider()
            SettingSwitch("Automatic AI model updates", "Unmetered network only. Disabled until you opt in to AI.", autoUpdate, enabled = aiEnabled) {
                viewModel.setAutoModelUpdateEnabled(it)
            }
        }

        SettingsCard("Local Transformer") {
            Text("Model", style = MaterialTheme.typography.labelLarge)
            Text("r3ddkahili/final-complete-malicious-url-model", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                if (viewModel.isModelLoaded()) "Runtime: ONNX Runtime • Loaded"
                else if (viewModel.isModelDownloaded()) "Runtime: ONNX Runtime • Downloaded"
                else "Runtime: ONNX Runtime • Not installed",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.loadModel() }, enabled = aiEnabled) { Text("Load") }
                Button(onClick = { viewModel.updateModel() }, enabled = aiEnabled) { Text("Update") }
                Button(onClick = { viewModel.deleteLocalModel() }) { Text("Clear") }
            }
            if (onNavigateToAi != null) {
                Spacer(Modifier.height(4.dp))
                Button(onClick = onNavigateToAi) { Text("Detailed AI Settings") }
            }
        }

        SettingsCard("Blocklists") {
            Text("Deterministic blocking remains the primary ad, tracker and domain protection layer.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { onNavigateToBlocklists?.invoke() }) { Text("Manage blocklists") }
        }

        SettingsCard("Browser & Qubes") {
            Text("Per-Qube profiles, browser privacy controls and disposable sessions are managed independently from the AI layer.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onNavigateToQubes?.invoke() }) { Text("Qubes") }
                if (onNavigateToFeedback != null) {
                    Button(onClick = onNavigateToFeedback) { Text("Feedback") }
                }
            }
        }

        SettingsCard("Privacy") {
            Text("No remote AI inference is used. AI classification runs locally when enabled.", style = MaterialTheme.typography.bodyMedium)
            Text("No Hugging Face API token is required.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    QubeGuardTheme { SettingsScreen() }
}
