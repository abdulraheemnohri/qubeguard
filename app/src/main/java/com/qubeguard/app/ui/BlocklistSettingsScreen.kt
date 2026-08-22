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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.qubeguard.app.data.blocklist.BlocklistSource
import com.qubeguard.app.ui.theme.QubeGuardTheme

/**
 * Blocklist Settings Screen for managing built-in and custom blocklist sources with category filtering.
 */
@Composable
fun BlocklistSettingsScreen() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val blocklistSources by viewModel.blocklistSources.observeAsState(emptyList())
    val totalRuleCount by viewModel.totalRuleCount.observeAsState(0)

    var newSourceName by remember { mutableStateOf("") }
    var newSourceUrl by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("all") }

    LaunchedEffect(Unit) {
        viewModel.loadBlocklistSources()
        viewModel.loadTotalRuleCount()
    }

    val categories = listOf("all", "ads", "privacy", "security", "social", "annoyances", "custom")
    val filteredSources = if (selectedCategoryFilter == "all") {
        blocklistSources
    } else {
        blocklistSources.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Blocklist Sources Management",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Active Rules", style = MaterialTheme.typography.labelLarge)
                    Text("$totalRuleCount rules", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Button(onClick = { viewModel.syncBlocklistsNow() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync")
                    Spacer(Modifier.width(6.dp))
                    Text("Sync Now")
                }
            }
        }

        // Category Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategoryFilter == cat,
                    onClick = { selectedCategoryFilter = cat },
                    label = { Text(cat.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        // Add custom source card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Add Custom Blocklist Source", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = newSourceName,
                    onValueChange = { newSourceName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Source Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = newSourceUrl,
                    onValueChange = { newSourceUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Source URL (txt/hosts format)") },
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (newSourceName.isNotBlank() && newSourceUrl.isNotBlank()) {
                            viewModel.addCustomBlocklistSource(newSourceName, newSourceUrl)
                            newSourceName = ""
                            newSourceUrl = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = newSourceName.isNotBlank() && newSourceUrl.isNotBlank()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    Spacer(Modifier.width(6.dp))
                    Text("Add Source")
                }
            }
        }

        Text("Subscribed Sources (${filteredSources.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredSources) { source ->
                BlocklistSourceItem(
                    source = source,
                    onToggle = { isEnabled ->
                        viewModel.setBlocklistSourceEnabled(source.id, isEnabled)
                    },
                    onDelete = if (source.id.startsWith("custom_")) {
                        { viewModel.deleteBlocklistSource(source.id) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun BlocklistSourceItem(
    source: BlocklistSource,
    onToggle: (Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Category: ${source.category} • Format: ${source.format}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (source.lastUpdated != null) {
                    Text(
                        text = "Updated: ${source.lastUpdated}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Switch(
                checked = source.enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BlocklistSettingsScreenPreview() {
    QubeGuardTheme {
        Surface {
            BlocklistSettingsScreen()
        }
    }
}
