package com.qubeguard.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.ui.theme.QubeGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
    onNavigateToAllowlist: (() -> Unit)? = null,
    onNavigateToQubes: (() -> Unit)? = null,
    onNavigateToAi: (() -> Unit)? = null,
    onNavigateToFeedback: (() -> Unit)? = null,
    onNavigateToDnsLogs: (() -> Unit)? = null
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val aiEnabled by viewModel.isMlEnabled.observeAsState(false)
    val autoUpdate by viewModel.isAutoModelUpdateEnabled.observeAsState(false)
    val upstreamDns by viewModel.upstreamDns.observeAsState("1.1.1.1")

    var showDnsMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var importJsonText by remember { mutableStateOf("") }

    val dnsProviders = mapOf(
        "1.1.1.1" to "Cloudflare (1.1.1.1)",
        "94.140.14.14" to "AdGuard DNS (94.140.14.14)",
        "9.9.9.9" to "Quad9 (9.9.9.9)",
        "8.8.8.8" to "Google DNS (8.8.8.8)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Privacy controls, network DNS, and local protection", style = MaterialTheme.typography.bodyMedium)

        SettingsCard("Protection") {
            SettingSwitch("AI malicious-URL protection", "Optional local Transformer; protection continues without it.", aiEnabled) {
                viewModel.setMlEnabled(it)
            }
            HorizontalDivider()
            SettingSwitch("Automatic AI model updates", "Unmetered network only. Disabled until you opt in to AI.", autoUpdate, enabled = aiEnabled) {
                viewModel.setAutoModelUpdateEnabled(it)
            }
        }

        SettingsCard("Network & Upstream DNS") {
            Text("Selected Upstream DNS", style = MaterialTheme.typography.labelLarge)
            Box {
                OutlinedButton(onClick = { showDnsMenu = true }) {
                    Text(dnsProviders[upstreamDns] ?: upstreamDns)
                }
                DropdownMenu(expanded = showDnsMenu, onDismissRequest = { showDnsMenu = false }) {
                    dnsProviders.forEach { (ip, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setUpstreamDns(ip)
                                showDnsMenu = false
                            }
                        )
                    }
                }
            }
            if (onNavigateToDnsLogs != null) {
                Spacer(Modifier.height(4.dp))
                Button(onClick = onNavigateToDnsLogs) { Text("View DNS Network Logs") }
            }
        }

        SettingsCard("Blocklists & Allowlist") {
            Text("Manage deterministic blocking sources and custom allowed domains.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onNavigateToBlocklists?.invoke() }) { Text("Blocklists") }
                if (onNavigateToAllowlist != null) {
                    OutlinedButton(onClick = onNavigateToAllowlist) { Text("Allowlist") }
                }
            }
        }

        SettingsCard("Backup & Restore") {
            Text("Backup or restore custom blocklist sources, allowlists, and DNS settings.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        exportedJsonText = viewModel.exportSettingsJson()
                        showExportDialog = true
                    }
                }) {
                    Text("Export Settings")
                }
                OutlinedButton(onClick = { showImportDialog = true }) {
                    Text("Import Settings")
                }
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

        SettingsCard("Browser & Qubes") {
            Text("Per-Qube profiles, browser privacy controls and disposable sessions are managed independently from the AI layer.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onNavigateToQubes?.invoke() }) { Text("Qubes") }
                if (onNavigateToFeedback != null) {
                    Button(onClick = onNavigateToFeedback) { Text("Feedback") }
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exported Settings JSON") },
            text = {
                OutlinedTextField(
                    value = exportedJsonText,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    maxLines = 8
                )
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Close") }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Settings JSON") },
            text = {
                OutlinedTextField(
                    value = importJsonText,
                    onValueChange = { importJsonText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Paste JSON config") },
                    maxLines = 8
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val success = viewModel.importSettingsJson(importJsonText)
                        if (success) {
                            Toast.makeText(context, "Settings imported successfully", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                            importJsonText = ""
                        } else {
                            Toast.makeText(context, "Invalid JSON settings payload", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
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
