package com.qubeguard.app.ui

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.browser.SecureWebView
import com.qubeguard.app.ui.theme.QubeGuardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BrowserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialUrl = intent?.dataString ?: intent?.getStringExtra("url") ?: "https://duckduckgo.com"
        setContent {
            QubeGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserScreen(initialUrl = initialUrl)
                }
            }
        }
    }
}

@Composable
fun BrowserScreen(initialUrl: String = "https://duckduckgo.com") {
    val viewModel: BrowserViewModel = hiltViewModel()
    val context = LocalContext.current
    val tabs by viewModel.tabs.observeAsState(emptyList())
    val activeTabId by viewModel.activeTabId.observeAsState("")
    val isDesktopMode by viewModel.isDesktopMode.observeAsState(false)
    val searchEngine by viewModel.searchEngine.observeAsState("DuckDuckGo")
    val loadProgress by viewModel.loadProgress.observeAsState(0)
    val selectedQube by viewModel.selectedQube.observeAsState()
    val qubes by viewModel.qubes.observeAsState(emptyList())
    val bookmarks by viewModel.bookmarks.observeAsState(emptyList())
    val history by viewModel.history.observeAsState(emptyList())

    var urlInput by remember { mutableStateOf(initialUrl) }
    var webView: SecureWebView? by remember { mutableStateOf(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showTabSwitcherDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showQubeDialog by remember { mutableStateOf(false) }
    var showSearchEngineDialog by remember { mutableStateOf(false) }

    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()

    LaunchedEffect(selectedQube) {
        selectedQube?.let { qube ->
            webView?.setQubeId(qube.id)
        }
    }

    LaunchedEffect(isDesktopMode) {
        webView?.setDesktopMode(isDesktopMode)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chrome-Style Top Action Bar (Omnibox, Lock, Tab Badge, Overflow Menu)
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { webView?.goBack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    // Chrome-Style Pill Omnibox
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure",
                                tint = if (urlInput.startsWith("https://")) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                val formatted = viewModel.formatSearchOrUrl(urlInput)
                                urlInput = formatted
                                viewModel.updateActiveTabUrl(formatted)
                                webView?.loadUrl(formatted)
                            }) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reload")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(Modifier.width(6.dp))

                    // Chrome-Style Tab Badge Button [ N ]
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(36.dp)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { showTabSwitcherDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabs.size.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                    }

                    // 3-Dot Chrome Overflow Menu
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New tab") },
                                onClick = {
                                    viewModel.addNewTab()
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Search Engine: $searchEngine") },
                                onClick = {
                                    showSearchEngineDialog = true
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Qube profile...") },
                                onClick = {
                                    showQubeDialog = true
                                    showOverflowMenu = false
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (isDesktopMode) "✓ Desktop site" else "Desktop site") },
                                onClick = {
                                    viewModel.toggleDesktopMode()
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add Bookmark") },
                                onClick = {
                                    val targetUrl = webView?.url ?: urlInput
                                    viewModel.addBookmark(webView?.title ?: targetUrl, targetUrl)
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Bookmarks (${bookmarks.size})") },
                                onClick = {
                                    showBookmarksDialog = true
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("History (${history.size})") },
                                onClick = {
                                    showHistoryDialog = true
                                    showOverflowMenu = false
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    context.startActivity(Intent(context, SettingsActivity::class.java))
                                    showOverflowMenu = false
                                }
                            )
                        }
                    }
                }

                // Page Loading Progress Bar
                if (loadProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { loadProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // WebView
        AndroidView(
            factory = { ctx ->
                SecureWebView(ctx).apply {
                    selectedQube?.let { setQubeId(it.id) }
                    setDesktopMode(isDesktopMode)
                    onProgressChanged = { progress ->
                        viewModel.setLoadProgress(progress)
                    }
                    onTitleReceived = { title ->
                        viewModel.updateActiveTabUrl(url ?: "", title)
                    }
                    webViewClient = object : WebViewClient() {
                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            url?.let {
                                this@apply.loadUrl(it)
                                urlInput = it
                                viewModel.updateActiveTabUrl(it)
                            }
                            return true
                        }
                    }
                    webView = this
                    loadUrl(activeTab?.url ?: initialUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // Search Engine Dialog
    if (showSearchEngineDialog) {
        AlertDialog(
            onDismissRequest = { showSearchEngineDialog = false },
            title = { Text("Choose Search Engine") },
            text = {
                Column {
                    listOf("DuckDuckGo", "StartPage", "Google", "Bing", "Ecosia").forEach { engine ->
                        TextButton(
                            onClick = {
                                viewModel.setSearchEngine(engine)
                                showSearchEngineDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(engine, fontWeight = if (engine == searchEngine) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchEngineDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Chrome Grid Tab Switcher Modal
    if (showTabSwitcherDialog) {
        AlertDialog(
            onDismissRequest = { showTabSwitcherDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tabs (${tabs.size})", fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        viewModel.addNewTab()
                        showTabSwitcherDialog = false
                    }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New tab")
                    }
                }
            },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs) { tab ->
                        val isSelected = tab.id == activeTabId
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.clickable {
                                viewModel.switchTab(tab.id)
                                urlInput = tab.url
                                webView?.loadUrl(tab.url)
                                showTabSwitcherDialog = false
                            }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tab.title.take(12),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        modifier = Modifier.height(20.dp).width(20.dp),
                                        onClick = { viewModel.closeTab(tab.id) }
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = tab.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTabSwitcherDialog = false }) { Text("Done") }
            }
        )
    }

    // Qube Profile Switcher Dialog
    if (showQubeDialog) {
        AlertDialog(
            onDismissRequest = { showQubeDialog = false },
            title = { Text("Select Qube Profile") },
            text = {
                LazyColumn {
                    items(qubes) { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectQube(q)
                                    showQubeDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(q.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            if (q.isIncognito) {
                                Spacer(Modifier.width(6.dp))
                                Text("(Incognito)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQubeDialog = false }) { Text("Close") }
            }
        )
    }

    // Bookmarks Dialog
    if (showBookmarksDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarksDialog = false },
            title = { Text("Bookmarks") },
            text = {
                if (bookmarks.isEmpty()) {
                    Text("No bookmarks saved yet.")
                } else {
                    LazyColumn {
                        items(bookmarks) { b ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        urlInput = b.url
                                        viewModel.updateActiveTabUrl(b.url, b.title)
                                        webView?.loadUrl(b.url)
                                        showBookmarksDialog = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(b.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(b.url, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { viewModel.deleteBookmark(b.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete bookmark")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarksDialog = false }) { Text("Close") }
            }
        )
    }

    // History Dialog
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Browsing History") },
            text = {
                if (history.isEmpty()) {
                    Text("No browsing history.")
                } else {
                    LazyColumn {
                        items(history) { h ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        urlInput = h.url
                                        viewModel.updateActiveTabUrl(h.url, h.title)
                                        webView?.loadUrl(h.url)
                                        showHistoryDialog = false
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(h.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(h.url, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearHistory() }) { Text("Clear History") }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryDialog = false }) { Text("Close") }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BrowserScreenPreview() {
    QubeGuardTheme {
        BrowserScreen()
    }
}
