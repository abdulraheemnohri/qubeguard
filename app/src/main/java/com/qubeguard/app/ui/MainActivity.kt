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
import androidx.compose.material3.ButtonDefaults
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
import com.qubeguard.app.engine.HealthState
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
    val health by viewModel.protectionHealth.observeAsState()
    val totalQueries by viewModel.totalQueries.observeAsState(0)
    val blockedQueries by viewModel.blockedQueries.observeAsState(0)
    val adsCount by viewModel.adsCount.observeAsState(0)
    val trackersCount by viewModel.trackersCount.observeAsState(0)
    val malwareCount by viewModel.malwareCount.observeAsState(0)
    val savedMb by viewModel.estimatedSavedMb.observeAsState("0.0 MB")
    val uptime by viewModel.formattedConnectionTime.observeAsState("00:00:00")

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (vpnRunning) "CONNECTED (${health?.state ?: HealthState.PROTECTED})" else "DISCONNECTED",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (vpnRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    if (vpnRunning) {
                        Text("Duration: $uptime", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(
                    if (vpnRunning) "VPN/DNS filtering is running. ${health?.activeRuleCount ?: 0} active rules loaded."
                    else "Start protection to enable system-wide ad, tracker, and DNS filtering.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (vpnRunning) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
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
                    Text(if (vpnRunning) "Disconnect Protection" else "Connect Protection")
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Total Requests", totalQueries.toString(), Modifier.weight(1f))
            StatCard("Blocked", blockedQueries.toString(), Modifier.weight(1f))
            StatCard("Data Saved", savedMb, Modifier.weight(1f))
        }

        // Protection Health Engine Monitor Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Protection Health Monitor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("VPN DNS Proxy:", style = MaterialTheme.typography.bodyMedium)
                    Text(if (health?.vpnActive == true) "Active" else "Paused", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Blocklists Loaded:", style = MaterialTheme.typography.bodyMedium)
                    Text("${health?.activeRuleCount ?: 0} rules", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Local AI Classifier:", style = MaterialTheme.typography.bodyMedium)
                    Text(if (health?.aiEngineReady == true) "Loaded" else "Not installed", fontWeight = FontWeight.Bold)
                }
            }
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
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Est. Bandwidth Saved:", style = MaterialTheme.typography.bodyMedium)
                    Text(savedMb, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
