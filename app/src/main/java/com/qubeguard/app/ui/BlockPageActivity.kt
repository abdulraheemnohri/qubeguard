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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.ui.theme.QubeGuardTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Block Page Activity for QubeGuard.
 * Displays an interstitial page when a URL is blocked, with options to allow or report.
 */
@AndroidEntryPoint
class BlockPageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QubeGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BlockPageScreen()
                }
            }
        }
    }
}

@Composable
fun BlockPageScreen() {
    val viewModel: BlockPageViewModel = hiltViewModel()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🛡️ Connection Blocked",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Domain: ${viewModel.blockedUrl}",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Reason: ${viewModel.blockReason}",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Confidence: ${(viewModel.confidence * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Category: ${viewModel.category}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.allowOnce() }
        ) {
            Text("Allow Once")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.allowAlways() }
        ) {
            Text("Allow Always (Add to Allowlist)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.keepBlocked() }
        ) {
            Text("Keep Blocked")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.reportFalsePositive() }
        ) {
            Text("Report False Positive")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BlockPageScreenPreview() {
    QubeGuardTheme {
        BlockPageScreen()
    }
}
