package com.qubeguard.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Switch
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
    private var customVideoView: View? = null
    private var customVideoCallback: WebChromeClient.CustomViewCallback? = null
    private var fullScreenContainer: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialUrl = intent?.dataString ?: intent?.getStringExtra("url") ?: "https://duckduckgo.com"

        fullScreenContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }

        setContent {
            QubeGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BrowserScreen(
                            initialUrl = initialUrl,
                            onShowVideo = { view, callback ->
                                customVideoView = view
                                customVideoCallback = callback
                                fullScreenContainer?.addView(view)
                                fullScreenContainer?.visibility = View.VISIBLE
                            },
                            onHideVideo = {
                                fullScreenContainer?.visibility = View.GONE
                                fullScreenContainer?.removeAllViews()
                                customVideoCallback?.onCustomViewHidden()
                                customVideoView = null
                                customVideoCallback = null
                            }
                        )

                        AndroidView(
                            factory = { fullScreenContainer!! },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (customVideoView != null) {
            fullScreenContainer?.visibility = View.GONE
            fullScreenContainer?.removeAllViews()
            customVideoCallback?.onCustomViewHidden()
            customVideoView = null
            customVideoCallback = null
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}

@Composable
fun BrowserScreen(
    initialUrl: String = "https://duckduckgo.com",
    onShowVideo: ((View, WebChromeClient.CustomViewCallback) -> Unit)? = null,
    onHideVideo: (() -> Unit)? = null
) {
    val viewModel: BrowserViewModel = hiltViewModel()
    val context = LocalContext.current
    val tabs by viewModel.tabs.observeAsState(emptyList())
    val activeTabId by viewModel.activeTabId.observeAsState("")
    val isDesktopMode by viewModel.isDesktopMode.observeAsState(false)
    val cosmeticAdHidingActive by viewModel.isCosmeticAdHidingEnabled.observeAsState(true)
    val isJsEnabled by viewModel.isJavaScriptEnabled.observeAsState(true)
    val blockPopups by viewModel.blockPopups.observeAsState(true)
    val textZoom by viewModel.textZoomLevel.observeAsState(100)
    val searchEngine by viewModel.searchEngine.observeAsState("DuckDuckGo")
    val loadProgress by viewModel.loadProgress.observeAsState(0)
    val selectedQube by viewModel.selectedQube.observeAsState()
    val qubes by viewModel.qubes.observeAsState(emptyList())
    val bookmarks by viewModel.bookmarks.observeAsState(emptyList())
    val history by viewModel.history.observeAsState(emptyList())
    val downloads by viewModel.downloads.observeAsState(emptyList())

    var urlInput by remember { mutableStateOf(initialUrl) }
    var webView: SecureWebView? by remember { mutableStateOf(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showTabSwitcherDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    var showShieldsDialog by remember { mutableStateOf(false) }
    var showQubeDialog by remember { mutableStateOf(false) }
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showBrowserSettingsDialog by remember { mutableStateOf(false) }

    // Video Download Prompt state
    var detectedMediaUrl by remember { mutableStateOf<String?>(null) }
    var detectedMediaMime by remember { mutableStateOf("") }
    var showVideoDownloadDialog by remember { mutableStateOf(false) }

    // Find in Page state
    var showFindInPage by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }

    // Reader Mode state
    var isReaderModeActive by remember { mutableStateOf(false) }
    var readerContentText by remember { mutableStateOf("") }

    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()

    LaunchedEffect(activeTabId, activeTab?.url) {
        val targetUrl = activeTab?.url ?: initialUrl
        urlInput = targetUrl
        webView?.let {
            if (it.url != targetUrl) {
                it.loadUrl(targetUrl)
            }
        }
    }

    LaunchedEffect(selectedQube) {
        selectedQube?.let { qube ->
            webView?.setQubeId(qube.id)
        }
    }

    LaunchedEffect(isDesktopMode) {
        webView?.setDesktopMode(isDesktopMode)
    }

    LaunchedEffect(cosmeticAdHidingActive) {
        webView?.isCosmeticAdHidingEnabled = cosmeticAdHidingActive
    }

    LaunchedEffect(isJsEnabled) {
        webView?.setJavaScriptEnabled(isJsEnabled)
    }

    LaunchedEffect(blockPopups) {
        webView?.setBlockPopups(blockPopups)
    }

    LaunchedEffect(textZoom) {
        webView?.setTextZoomLevel(textZoom)
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
                                tint = if (urlInput.startsWith("https://")) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.clickable { showShieldsDialog = true }
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
                                text = { Text("Find in Page") },
                                onClick = {
                                    showFindInPage = true
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reader Mode") },
                                onClick = {
                                    webView?.evaluateJavascript("(function(){ return document.body.innerText; })();") { text ->
                                        readerContentText = text ?: "No article text found."
                                        isReaderModeActive = true
                                    }
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
                                text = { Text("Browser Shields & Privacy") },
                                onClick = {
                                    showShieldsDialog = true
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Browser Settings") },
                                onClick = {
                                    showBrowserSettingsDialog = true
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Downloads (${downloads.size})") },
                                onClick = {
                                    showDownloadsDialog = true
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
                                text = { Text("System Settings") },
                                onClick = {
                                    context.startActivity(Intent(context, SettingsActivity::class.java))
                                    showOverflowMenu = false
                                }
                            )
                        }
                    }
                }

                // Find in Page Bar
                if (showFindInPage) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = findQuery,
                            onValueChange = {
                                findQuery = it
                                webView?.findAllAsync(it)
                            },
                            placeholder = { Text("Find on page...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
                        )
                        IconButton(onClick = { webView?.findNext(true) }) {
                            Text("↓", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        IconButton(onClick = {
                            webView?.clearMatches()
                            showFindInPage = false
                            findQuery = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Find")
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

        // Reader Mode Overlay
        if (isReaderModeActive) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reader Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { isReaderModeActive = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Reader Mode")
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(readerContentText, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)
            }
        } else {
            // WebView Container
            AndroidView(
                factory = { ctx ->
                    SecureWebView(ctx).apply {
                        selectedQube?.let { setQubeId(it.id) }
                        setDesktopMode(isDesktopMode)
                        isCosmeticAdHidingEnabled = cosmeticAdHidingActive
                        setJavaScriptEnabled(isJsEnabled)
                        setBlockPopups(blockPopups)
                        setTextZoomLevel(textZoom)
                        onProgressChanged = { progress ->
                            viewModel.setLoadProgress(progress)
                        }
                        onTitleReceived = { title ->
                            viewModel.updateActiveTabUrl(url ?: "", title)
                        }
                        onUrlLoadingListener = { newUrl ->
                            urlInput = newUrl
                            viewModel.updateActiveTabUrl(newUrl)
                        }
                        onShowCustomViewListener = { view, callback ->
                            onShowVideo?.invoke(view, callback)
                        }
                        onHideCustomViewListener = {
                            onHideVideo?.invoke()
                        }
                        onMediaDetectedListener = { mediaUrl, mime ->
                            detectedMediaUrl = mediaUrl
                            detectedMediaMime = mime
                            showVideoDownloadDialog = true
                        }
                        onDownloadTriggered = { downloadUrl, _, _, mime, len ->
                            val fileName = Uri.parse(downloadUrl).lastPathSegment ?: "file"
                            viewModel.addDownload(downloadUrl, fileName, len, mime)
                            Toast.makeText(ctx, "Download started: $fileName", Toast.LENGTH_SHORT).show()
                        }
                        webView = this
                        loadUrl(activeTab?.url ?: initialUrl)
                    }
                },
                update = { view ->
                    val targetUrl = activeTab?.url ?: initialUrl
                    if (view.url != targetUrl) {
                        view.loadUrl(targetUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Detailed Browser Settings Modal
    if (showBrowserSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showBrowserSettingsDialog = false },
            title = { Text("Browser Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable JavaScript", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Allows interactive Web content", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = isJsEnabled, onCheckedChange = { viewModel.toggleJavaScript() })
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Block Popups", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Prevents unauthorized popup windows", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = blockPopups, onCheckedChange = { viewModel.toggleBlockPopups() })
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Do Not Track Header", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Sends DNT HTTP header to websites", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = true, onCheckedChange = {}, enabled = false)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBrowserSettingsDialog = false }) { Text("Done") }
            }
        )
    }

    // Video Download Dialog Prompt
    if (showVideoDownloadDialog && detectedMediaUrl != null) {
        AlertDialog(
            onDismissRequest = { showVideoDownloadDialog = false },
            title = { Text("Video / Media Detected") },
            text = {
                Column {
                    Text("Found downloadable video resource:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(detectedMediaUrl ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val url = detectedMediaUrl ?: ""
                    val fileName = Uri.parse(url).lastPathSegment ?: "video.mp4"
                    viewModel.addDownload(url, fileName, 0, detectedMediaMime)
                    Toast.makeText(context, "Video download queued: $fileName", Toast.LENGTH_SHORT).show()
                    showVideoDownloadDialog = false
                }) {
                    Text("Download Video")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVideoDownloadDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Shields & Privacy Modal
    if (showShieldsDialog) {
        AlertDialog(
            onDismissRequest = { showShieldsDialog = false },
            title = { Text("Browser Shields & Protection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cosmetic Ad Element Hiding", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Injects CSS rules to hide leftover ad banners", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = cosmeticAdHidingActive,
                            onCheckedChange = { viewModel.toggleCosmeticAdHiding() }
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Third-Party Cookies Blocked", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Enforced globally in WebView settings", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = true, onCheckedChange = {}, enabled = false)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShieldsDialog = false }) { Text("Close") }
            }
        )
    }

    // Downloads Manager Dialog
    if (showDownloadsDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadsDialog = false },
            title = { Text("Downloads Manager") },
            text = {
                if (downloads.isEmpty()) {
                    Text("No file downloads recorded.")
                } else {
                    LazyColumn {
                        items(downloads) { d ->
                            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                Text(d.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("${d.mimeType} • ${d.sizeBytes / 1024} KB", style = MaterialTheme.typography.bodySmall)
                                Text(d.url, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDownloadsDialog = false }) { Text("Close") }
            }
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
