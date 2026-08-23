package com.qubeguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import com.qubeguard.app.data.blocklist.SystemLogEntity
import com.qubeguard.app.ui.theme.QubeGuardTheme

@Composable
fun SystemLogScreen() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val logs by viewModel.systemLogs.observeAsState(emptyList())

    var categoryFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadSystemLogs()
    }

    val filteredLogs = remember(logs, categoryFilter, searchQuery) {
        logs.filter { log ->
            val matchCategory = categoryFilter == "ALL" || log.category.equals(categoryFilter, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() || log.event.contains(searchQuery, ignoreCase = true) || log.details.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("System Event Logs", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Recorded app events: VPN, DNS, Browser & AI", style = MaterialTheme.typography.bodyMedium)
            }
            Row {
                IconButton(onClick = { viewModel.loadSystemLogs() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = { viewModel.clearSystemLogs() }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear logs")
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search logs...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = categoryFilter == "ALL", onClick = { categoryFilter = "ALL" }, label = { Text("All (${logs.size})") })
            FilterChip(selected = categoryFilter == "VPN", onClick = { categoryFilter = "VPN" }, label = { Text("VPN") })
            FilterChip(selected = categoryFilter == "DNS", onClick = { categoryFilter = "DNS" }, label = { Text("DNS") })
            FilterChip(selected = categoryFilter == "BROWSER", onClick = { categoryFilter = "BROWSER" }, label = { Text("Browser") })
        }

        if (filteredLogs.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No system event logs recorded.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    SystemLogItem(log = log)
                }
            }
        }
    }
}

@Composable
fun SystemLogItem(log: SystemLogEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[${log.category}] ${log.event}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = log.timestamp,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (log.details.isNotBlank()) {
                Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SystemLogScreenPreview() {
    QubeGuardTheme {
        Surface {
            SystemLogScreen()
        }
    }
}
