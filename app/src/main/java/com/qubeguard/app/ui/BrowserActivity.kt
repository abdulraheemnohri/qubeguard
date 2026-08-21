package com.qubeguard.app.ui

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    var showQubeDropdown by remember { mutableStateOf(false) }
    var showEngineDropdown by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

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
        // Navigation, Qube & Tabs bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { webView?.goBack() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            IconButton(onClick = { webView?.goForward() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
            }

            IconButton(onClick = { webView?.reload() }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
            }

            // Search Engine selector
            Box {
                OutlinedButton(onClick = { showEngineDropdown = true }) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Engine")
                    Spacer(Modifier.width(4.dp))
                    Text(searchEngine)
                }
                DropdownMenu(expanded = showEngineDropdown, onDismissRequest = { showEngineDropdown = false }) {
                    listOf("DuckDuckGo", "StartPage", "Google", "Bing", "Ecosia").forEach { engine ->
                        DropdownMenuItem(
                            text = { Text(engine) },
                            onClick = {
                                viewModel.setSearchEngine(engine)
                                showEngineDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Qube profile dropdown selector
            Box {
                OutlinedButton(onClick = { showQubeDropdown = true }) {
                    Text(selectedQube?.name ?: "Default")
                }
                DropdownMenu(expanded = showQubeDropdown, onDismissRequest = { showQubeDropdown = false }) {
                    qubes.forEach { qube ->
                        DropdownMenuItem(
                            text = { Text(qube.name + if (qube.isIncognito) " (Incognito)" else "") },
                            onClick = {
                                viewModel.selectQube(qube)
                                showQubeDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // Tabs bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tabs) { tab ->
                val isSelected = tab.id == activeTabId
                Card(
                    modifier = Modifier.clickable {
                        viewModel.switchTab(tab.id)
                        urlInput = tab.url
                        webView?.loadUrl(tab.url)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tab.title.take(15) + if (tab.title.length > 15) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            modifier = Modifier.height(18.dp).width(18.dp),
                            onClick = { viewModel.closeTab(tab.id) }
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close tab")
                        }
                    }
                }
            }

            item {
                IconButton(onClick = { viewModel.addNewTab() }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "New tab")
                }
            }
        }

        // Address Input Bar & Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f),
                label = { Text("Search or type URL") },
                singleLine = true
            )

            Spacer(Modifier.width(6.dp))

            Button(onClick = {
                val formatted = viewModel.formatSearchOrUrl(urlInput)
                urlInput = formatted
                viewModel.updateActiveTabUrl(formatted)
                webView?.loadUrl(formatted)
            }) {
                Text("Go")
            }

            IconButton(onClick = {
                val targetUrl = webView?.url ?: urlInput
                if (targetUrl.isNotBlank()) {
                    viewModel.addBookmark(webView?.title ?: targetUrl, targetUrl)
                }
            }) {
                Icon(imageVector = Icons.Default.Star, contentDescription = "Bookmark")
            }

            OutlinedButton(onClick = { viewModel.toggleDesktopMode() }) {
                Text(if (isDesktopMode) "Desktop" else "Mobile")
            }
        }

        // Page Progress Indicator
        if (loadProgress in 1..99) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Action Toolbar (Bookmarks & History buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { showBookmarksDialog = true }) { Text("Bookmarks (${bookmarks.size})") }
            TextButton(onClick = { showHistoryDialog = true }) { Text("History (${history.size})") }
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
