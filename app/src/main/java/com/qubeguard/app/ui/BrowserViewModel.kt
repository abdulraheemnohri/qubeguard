package com.qubeguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.qubeguard.app.browser.BookmarkEntity
import com.qubeguard.app.browser.HistoryEntity
import com.qubeguard.app.browser.QubeDao
import com.qubeguard.app.browser.QubeManager
import com.qubeguard.app.browser.QubeProfile
import com.qubeguard.app.engine.BlockingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    var url: String = "https://duckduckgo.com",
    var title: String = "New Tab"
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    application: Application,
    private val qubeManager: QubeManager,
    private val qubeDao: QubeDao,
    private val blockingEngine: BlockingEngine
) : AndroidViewModel(application) {

    private val _tabs = MutableLiveData<List<BrowserTab>>(listOf(BrowserTab()))
    val tabs: LiveData<List<BrowserTab>> = _tabs

    private val _activeTabId = MutableLiveData(_tabs.value?.first()?.id ?: "")
    val activeTabId: LiveData<String> = _activeTabId

    private val _isDesktopMode = MutableLiveData(false)
    val isDesktopMode: LiveData<Boolean> = _isDesktopMode

    private val _searchEngine = MutableLiveData("DuckDuckGo")
    val searchEngine: LiveData<String> = _searchEngine

    private val _loadProgress = MutableLiveData(0)
    val loadProgress: LiveData<Int> = _loadProgress

    private val _selectedQube = MutableLiveData<QubeProfile?>(null)
    val selectedQube: LiveData<QubeProfile?> = _selectedQube

    private val _qubes = MutableLiveData<List<QubeProfile>>(emptyList())
    val qubes: LiveData<List<QubeProfile>> = _qubes

    private val _bookmarks = MutableLiveData<List<BookmarkEntity>>(emptyList())
    val bookmarks: LiveData<List<BookmarkEntity>> = _bookmarks

    private val _history = MutableLiveData<List<HistoryEntity>>(emptyList())
    val history: LiveData<List<HistoryEntity>> = _history

    init {
        loadQubes()
    }

    private fun loadQubes() {
        viewModelScope.launch {
            _qubes.value = qubeManager.getAllQubes()
            val defaultQube = qubeManager.getDefaultQube()
            _selectedQube.value = defaultQube
            defaultQube?.let {
                loadBookmarks(it.id)
                loadHistory(it.id)
            }
        }
    }

    fun selectQube(qube: QubeProfile) {
        _selectedQube.value = qube
        loadBookmarks(qube.id)
        loadHistory(qube.id)
    }

    fun setLoadProgress(progress: Int) {
        _loadProgress.value = progress
    }

    fun toggleDesktopMode() {
        _isDesktopMode.value = !(_isDesktopMode.value ?: false)
    }

    fun setSearchEngine(engine: String) {
        _searchEngine.value = engine
    }

    fun addNewTab(url: String = "https://duckduckgo.com") {
        val newTab = BrowserTab(url = url)
        val currentTabs = _tabs.value.orEmpty() + newTab
        _tabs.value = currentTabs
        _activeTabId.value = newTab.id
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value.orEmpty().filterNot { it.id == tabId }
        if (currentTabs.isEmpty()) {
            val defaultTab = BrowserTab()
            _tabs.value = listOf(defaultTab)
            _activeTabId.value = defaultTab.id
        } else {
            _tabs.value = currentTabs
            if (_activeTabId.value == tabId) {
                _activeTabId.value = currentTabs.last().id
            }
        }
    }

    fun switchTab(tabId: String) {
        _activeTabId.value = tabId
    }

    fun updateActiveTabUrl(url: String, title: String? = null) {
        val activeId = _activeTabId.value ?: return
        val currentTabs = _tabs.value.orEmpty().map { tab ->
            if (tab.id == activeId) {
                tab.copy(url = url, title = title ?: tab.title)
            } else tab
        }
        _tabs.value = currentTabs

        val qube = _selectedQube.value ?: return
        if (!qube.isIncognito && url.isNotBlank() && !url.startsWith("about:")) {
            addHistory(qube.id, title ?: url, url)
        }
    }

    fun formatSearchOrUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        if (trimmed.contains(".") && !trimmed.contains(" ")) return "https://$trimmed"

        val query = java.net.URLEncoder.encode(trimmed, "UTF-8")
        return when (_searchEngine.value) {
            "StartPage" -> "https://www.startpage.com/sp/search?query=$query"
            "Google" -> "https://www.google.com/search?q=$query"
            "Bing" -> "https://www.bing.com/search?q=$query"
            "Ecosia" -> "https://www.ecosia.org/search?q=$query"
            else -> "https://duckduckgo.com/?q=$query"
        }
    }

    fun addBookmark(title: String, url: String) {
        val qubeId = _selectedQube.value?.id ?: QubeProfile.DEFAULT_QUBE_ID
        viewModelScope.launch {
            val bookmark = BookmarkEntity(
                id = UUID.randomUUID().toString(),
                qubeId = qubeId,
                title = title,
                url = url,
                createdAt = System.currentTimeMillis().toString()
            )
            qubeDao.insertBookmark(bookmark)
            loadBookmarks(qubeId)
        }
    }

    fun deleteBookmark(bookmarkId: String) {
        val qubeId = _selectedQube.value?.id ?: QubeProfile.DEFAULT_QUBE_ID
        viewModelScope.launch {
            qubeDao.deleteBookmark(bookmarkId)
            loadBookmarks(qubeId)
        }
    }

    private fun loadBookmarks(qubeId: String) {
        viewModelScope.launch {
            _bookmarks.value = qubeDao.getBookmarksByQube(qubeId)
        }
    }

    private fun addHistory(qubeId: String, title: String, url: String) {
        viewModelScope.launch {
            val entry = HistoryEntity(
                id = UUID.randomUUID().toString(),
                qubeId = qubeId,
                title = title,
                url = url,
                visitedAt = System.currentTimeMillis().toString()
            )
            qubeDao.insertHistory(entry)
            loadHistory(qubeId)
        }
    }

    private fun loadHistory(qubeId: String) {
        viewModelScope.launch {
            _history.value = qubeDao.getHistoryByQube(qubeId)
        }
    }

    fun clearHistory() {
        val qubeId = _selectedQube.value?.id ?: return
        viewModelScope.launch {
            qubeDao.clearHistoryForQube(qubeId)
            loadHistory(qubeId)
        }
    }

    suspend fun isBlocked(url: String): Boolean = blockingEngine.isBlocked(url)
}
