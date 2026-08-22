package com.qubeguard.app.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.ui.theme.QubeGuardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val serviceIntent = VpnService.prepare(this)
            if (serviceIntent == null) {
                startVpnDirectly()
            }
        } else {
            Toast.makeText(this, "VPN permission is required for network protection", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QubeGuardTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QubeGuardNavHost()
                }
            }
        }
    }

    fun requestVpnPermissionAndStart() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnDirectly()
        }
    }

    private fun startVpnDirectly() {
        val serviceIntent = Intent(this, com.qubeguard.app.engine.QubeGuardService::class.java).apply {
            action = com.qubeguard.app.engine.QubeGuardService.ACTION_START
        }
        startService(serviceIntent)
        Toast.makeText(this, "QubeGuard VPN protection started", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MainScreen(
    onNavigateToBrowser: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val viewModel: MainViewModel = hiltViewModel()
    val context = LocalContext.current
    val vpnRunning by viewModel.isVpnRunning.observeAsState(false)
    val totalQueries by viewModel.totalQueries.observeAsState(0)
    val blockedQueries by viewModel.blockedQueries.observeAsState(0)
    val adsCount by viewModel.adsCount.observeAsState(0)
    val trackersCount by viewModel.trackersCount.observeAsState(0)
    val malwareCount by viewModel.malwareCount.observeAsState(0)

    LaunchedEffect(Unit) {
        viewModel.loadAnalytics()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("QubeGuard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Private protection for your Android device", style = MaterialTheme.typography.bodyMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (vpnRunning) "Protection active" else "Protection paused",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (vpnRunning) "VPN/DNS filtering is running. System-wide network requests are protected."
                    else "Start protection to enable network-level filtering.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (vpnRunning) {
                            viewModel.stopVpn()
                        } else {
                            if (context is MainActivity) {
                                context.requestVpnPermissionAndStart()
                            } else {
                                viewModel.startVpn()
                            }
                        }
                    }
                ) {
                    Text(if (vpnRunning) "Stop protection" else "Start protection")
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Total Requests", totalQueries.toString(), Modifier.weight(1f))
            StatCard("Blocked", blockedQueries.toString(), Modifier.weight(1f))
        }

        // Threat Analytics Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Threat Analytics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ads Blocked:", style = MaterialTheme.typography.bodyMedium)
                    Text("$adsCount", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Trackers Prevented:", style = MaterialTheme.typography.bodyMedium)
                    Text("$trackersCount", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Malware/Phishing Blocked:", style = MaterialTheme.typography.bodyMedium)
                    Text("${kotlin.math.max(0, malwareCount)}", fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        if (onNavigateToBrowser != null) onNavigateToBrowser()
                        else context.startActivity(Intent(context, BrowserActivity::class.java))
                    }) {
                        Text("Private browser")
                    }
                    OutlinedButton(onClick = {
                        if (onNavigateToSettings != null) onNavigateToSettings()
                        else context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Text("Settings")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Protection layers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Layer 1  •  Deterministic blocklists", style = MaterialTheme.typography.bodyMedium)
                Text("Layer 2  •  Local VPN / DNS filtering", style = MaterialTheme.typography.bodyMedium)
                Text("Layer 3  •  Optional local Transformer AI", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("QubeGuard • Local-first privacy", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
