package com.qubeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.ml.ModelDownloader
import com.qubeguard.app.ui.theme.QubeGuardTheme

/** Local-only Transformer model controls. */
@Composable
fun MlSettingsScreen() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val isMlEnabled by viewModel.isMlEnabled.observeAsState(true)
    val isModelLoaded = viewModel.isModelLoaded()
    val isModelDownloaded = viewModel.isModelDownloaded()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("AI & Model", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "QubeGuard runs the malicious-URL Transformer entirely on this device.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Layer 3 Transformer", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isMlEnabled,
                onCheckedChange = { if (it) viewModel.enableLocalModel() else viewModel.disableLocalModel() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Source model", style = MaterialTheme.typography.titleMedium)
        Text(ModelDownloader.SOURCE_MODEL, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Classes: Benign, Defacement, Phishing, Malware",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Runtime: ONNX Runtime Android • max sequence length: 128 • INT8 mobile export",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            when {
                isModelLoaded -> "Model status: Loaded locally"
                isModelDownloaded -> "Model status: Downloaded; load pending"
                else -> "Model status: Not downloaded"
            },
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { viewModel.loadModel() }, modifier = Modifier.fillMaxWidth()) {
            Text("Load / Download Model")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.updateModel() }, modifier = Modifier.fillMaxWidth()) {
            Text("Force Model Update")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MlSettingsScreenPreview() {
    QubeGuardTheme { Surface { MlSettingsScreen() } }
}
