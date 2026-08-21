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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.ui.theme.QubeGuardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockPageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QubeGuardTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    BlockPageScreen()
                }
            }
        }
    }
}

@Composable
fun BlockPageScreen() {
    val viewModel: BlockPageViewModel = hiltViewModel()
    val blockedUrl by viewModel.blockedUrl.observeAsState("")
    val reason by viewModel.blockReason.observeAsState("")
    val confidence by viewModel.confidence.observeAsState(0f)
    val category by viewModel.category.observeAsState("")

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🛡️ Connection Blocked", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("Domain: $blockedUrl", style = MaterialTheme.typography.bodyMedium)
        Text("Reason: $reason", style = MaterialTheme.typography.bodyMedium)
        Text("Confidence: ${(confidence * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
        Text("Category: $category", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))
        Button(onClick = viewModel::allowOnce) { Text("Allow Once") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = viewModel::allowAlways) { Text("Allow Always") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = viewModel::keepBlocked) { Text("Keep Blocked") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = viewModel::reportFalsePositive) { Text("Report False Positive") }
    }
}
