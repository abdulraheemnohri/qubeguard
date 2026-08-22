package com.qubeguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.data.blocklist.BlocklistRule
import com.qubeguard.app.ui.theme.QubeGuardTheme

/**
 * Screen for managing custom allowlisted domains.
 */
@Composable
fun AllowlistSettingsScreen() {
    val viewModel: AllowlistViewModel = hiltViewModel()
    val allowlistRules by viewModel.allowlistRules.observeAsState(emptyList())
    var domainInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadAllowlistRules()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Custom Allowlist", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Always allow connections to these domains regardless of blocklists or AI.", style = MaterialTheme.typography.bodyMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = domainInput,
                    onValueChange = { domainInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Domain (e.g. example.com)") },
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (domainInput.isNotBlank()) {
                            viewModel.addAllowlistRule(domainInput)
                            domainInput = ""
                        }
                    },
                    enabled = domainInput.isNotBlank()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    Spacer(Modifier.width(4.dp))
                    Text("Allow")
                }
            }
        }

        Text("Allowed Domains (${allowlistRules.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        if (allowlistRules.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No custom allowlisted domains. Domains added here will never be blocked.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allowlistRules) { rule ->
                    AllowlistItem(rule = rule, onDelete = { viewModel.removeAllowlistRule(rule.id) })
                }
            }
        }
    }
}

@Composable
fun AllowlistItem(rule: BlocklistRule, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(rule.rule, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AllowlistSettingsScreenPreview() {
    QubeGuardTheme {
        Surface {
            AllowlistSettingsScreen()
        }
    }
}
