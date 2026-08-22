package com.qubeguard.app.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.ui.theme.QubeGuardTheme

data class AppInfoItem(
    val name: String,
    val packageName: String
)

/**
 * Screen for configuring per-app VPN bypass (split tunneling).
 */
@Composable
fun AppBypassScreen() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val context = LocalContext.current
    val bypassPackages by viewModel.bypassPackages.observeAsState(emptySet())

    val installedApps = remember {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        packages.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { AppInfoItem(name = pm.getApplicationLabel(it).toString(), packageName = it.packageName) }
            .sortedBy { it.name.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Per-App Split Tunneling", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Select applications to bypass VPN DNS protection.", style = MaterialTheme.typography.bodyMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Apps enabled below will connect directly to the internet without passing through QubeGuard's local DNS filter.",
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text("Installed User Applications (${installedApps.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(installedApps) { app ->
                val isBypassed = bypassPackages.contains(app.packageName)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = isBypassed,
                            onCheckedChange = { checked ->
                                viewModel.setAppBypass(app.packageName, checked)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppBypassScreenPreview() {
    QubeGuardTheme {
        Surface {
            AppBypassScreen()
        }
    }
}
