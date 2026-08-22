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
import com.qubeguard.app.data.blocklist.BlocklistRule
import com.qubeguard.app.ui.theme.QubeGuardTheme

/**
 * Screen for managing custom user-defined rules (blocking or allowing domains and regex patterns).
 */
@Composable
fun CustomRulesScreen() {
    val viewModel: AllowlistViewModel = hiltViewModel()
    val allowlistRules by viewModel.allowlistRules.observeAsState(emptyList())

    var rulePattern by remember { mutableStateOf("") }
    var isAllowlistRule by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.loadAllowlistRules()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Custom Rules Manager", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Create custom domain, URL, or regex blocking and allow rules.", style = MaterialTheme.typography.bodyMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Add Custom Rule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = rulePattern,
                    onValueChange = { rulePattern = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pattern (e.g. example.com or ||ads.*^)") },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAllowlistRule) "Rule Type: ALLOW (Whitelist)" else "Rule Type: BLOCK (Custom Blacklist)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = isAllowlistRule,
                        onCheckedChange = { isAllowlistRule = it }
                    )
                }

                Button(
                    onClick = {
                        if (rulePattern.isNotBlank()) {
                            viewModel.addAllowlistRule(rulePattern)
                            rulePattern = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = rulePattern.isNotBlank()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    Spacer(Modifier.width(6.dp))
                    Text("Save Rule")
                }
            }
        }

        Text("Active User Rules (${allowlistRules.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        if (allowlistRules.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No custom user rules defined.",
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
                    CustomRuleItem(rule = rule, onDelete = { viewModel.removeAllowlistRule(rule.id) })
                }
            }
        }
    }
}

@Composable
fun CustomRuleItem(rule: BlocklistRule, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.rule, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("Type: ${rule.type} • ${if (rule.isAllowlist) "ALLOW" else "BLOCK"}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomRulesScreenPreview() {
    QubeGuardTheme {
        Surface {
            CustomRulesScreen()
        }
    }
}
