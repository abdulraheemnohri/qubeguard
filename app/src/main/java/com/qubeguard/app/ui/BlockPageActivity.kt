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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.ui.theme.QubeGuardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockPageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent?.getStringExtra(EXTRA_BLOCKED_URL) ?: "unknown"
        setContent {
            QubeGuardTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    BlockPageScreen(blockedUrl = url, onFinish = { finish() })
                }
            }
        }
    }

    companion object {
        const val EXTRA_BLOCKED_URL = "extra_blocked_url"
    }
}

@Composable
fun BlockPageScreen(
    blockedUrl: String = "",
    onFinish: () -> Unit = {}
) {
    val viewModel: BlockPageViewModel = hiltViewModel()
    val currentUrl by viewModel.blockedUrl.observeAsState(blockedUrl)
    val reason by viewModel.blockReason.observeAsState("")
    val confidence by viewModel.confidence.observeAsState(0f)
    val category by viewModel.category.observeAsState("")

    LaunchedEffect(blockedUrl) {
        if (blockedUrl.isNotBlank()) {
            viewModel.initialize(blockedUrl)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🛡️ Connection Blocked",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Text(
                    text = "QubeGuard prevented access to this location for your security and privacy.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(8.dp))

                Text("Domain / URL: $currentUrl", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("Reason: $reason", style = MaterialTheme.typography.bodyMedium)
                if (confidence > 0f) {
                    Text("Confidence: ${(confidence * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                }
                Text("Category: $category", style = MaterialTheme.typography.bodyMedium)

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.allowOnce()
                            onFinish()
                        }
                    ) {
                        Text("Allow Once")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.allowAlways()
                            onFinish()
                        }
                    ) {
                        Text("Allow Always")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.keepBlocked()
                            onFinish()
                        }
                    ) {
                        Text("Keep Blocked")
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.reportFalsePositive()
                            onFinish()
                        }
                    ) {
                        Text("Report FP")
                    }
                }
            }
        }
    }
}
