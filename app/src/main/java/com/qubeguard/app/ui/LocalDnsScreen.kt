package com.qubeguard.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LocalDnsScreen() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val context = LocalContext.current

    val records by viewModel.localDnsRecords.observeAsState(emptyList())
    val condEnabled by viewModel.conditionalForwardingEnabled.observeAsState(false)
    val condDomain by viewModel.conditionalDomain.observeAsState("home.arpa")
    val condIp by viewModel.conditionalTargetIp.observeAsState("192.168.1.1")

    var showAddDialog by remember { mutableStateOf(false) }
    var domainInput by remember { mutableStateOf("") }
    var ipInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }

    var localCondDomain by remember { mutableStateOf(condDomain) }
    var localCondIp by remember { mutableStateOf(condIp) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Local DNS Records", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Pi-hole style custom A / AAAA DNS mappings", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Record")
                Spacer(Modifier.padding(horizontal = 2.dp))
                Text("Add")
            }
        }

        // Conditional Forwarding Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Conditional Forwarding", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = condEnabled,
                        onCheckedChange = { viewModel.setConditionalForwarding(it, localCondDomain, localCondIp) }
                    )
                }
                Text("Route queries for local domain to a specific LAN DNS router.", style = MaterialTheme.typography.bodySmall)

                if (condEnabled) {
                    OutlinedTextField(
                        value = localCondDomain,
                        onValueChange = {
                            localCondDomain = it
                            viewModel.setConditionalForwarding(true, localCondDomain, localCondIp)
                        },
                        label = { Text("Local Domain (e.g. home.arpa)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = localCondIp,
                        onValueChange = {
                            localCondIp = it
                            viewModel.setConditionalForwarding(true, localCondDomain, localCondIp)
                        },
                        label = { Text("Target IP (e.g. 192.168.1.1)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        Text("Active Custom Records (${records.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        if (records.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    "No local DNS records added yet. Click Add to create local hostname overrides (e.g., router.local -> 192.168.1.1).",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(records, key = { it.id }) { rec ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.domain, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("→ ${rec.ipAddress} (${rec.recordType})", style = MaterialTheme.typography.bodyMedium)
                                if (rec.description.isNotBlank()) {
                                    Text(rec.description, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteLocalDnsRecord(rec.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Local DNS Record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = domainInput,
                        onValueChange = { domainInput = it },
                        label = { Text("Hostname / Domain") },
                        placeholder = { Text("nas.home.arpa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        label = { Text("IP Address") },
                        placeholder = { Text("192.168.1.50") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Description (Optional)") },
                        placeholder = { Text("Home NAS Server") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (domainInput.isNotBlank() && ipInput.isNotBlank()) {
                        viewModel.addLocalDnsRecord(domainInput, ipInput, "A", descInput)
                        showAddDialog = false
                        domainInput = ""
                        ipInput = ""
                        descInput = ""
                        Toast.makeText(context, "Local DNS record added", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
