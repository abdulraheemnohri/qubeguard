package com.qubeguard.app.browser

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.qubeguard.app.data.blocklist.DeterministicBlocker
import com.qubeguard.app.ml.MLClassifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class SecureWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle
) : WebView(context, attrs, defStyleAttr) {

    @Inject lateinit var deterministicBlocker: DeterministicBlocker
    @Inject lateinit var mlClassifier: MLClassifier

    private var qubeId: String = "default"
    var isCosmeticAdHidingEnabled: Boolean = true
    var onProgressChanged: ((Int) -> Unit)? = null
    var onTitleReceived: ((String) -> Unit)? = null
    var onUrlLoadingListener: ((String) -> Unit)? = null
    var onDownloadTriggered: ((url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long) -> Unit)? = null
    var onShowCustomViewListener: ((View, WebChromeClient.CustomViewCallback) -> Unit)? = null
    var onHideCustomViewListener: (() -> Unit)? = null
    var onMediaDetectedListener: ((mediaUrl: String, mimeType: String) -> Unit)? = null

    companion object {
        const val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Mobile Safari/537.36"
        const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        private const val COSMETIC_AD_BLOCK_SCRIPT = """
            (function() {
                var selectors = [
                    'iframe[src*="ads"]', 'iframe[src*="doubleclick"]',
                    'div[id*="google_ads"]', 'div[class*="ad-banner"]',
                    'div[class*="ad-container"]', '.ad-slot', '.sponsored-post',
                    '.ad-wrapper', '.ad-box', 'ins.adsbygoogle', '[class*="sponsored"]'
                ];
                var style = document.createElement('style');
                style.innerHTML = selectors.join(', ') + ' { display: none !important; visibility: hidden !important; height: 0 !important; opacity: 0 !important; pointer-events: none !important; }';
                document.head.appendChild(style);
            })();
        """
    }

    init {
        initializeWebView()
    }

    private fun initializeWebView() {
        settings.javaScriptEnabled = true
        settings.setGeolocationEnabled(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.domStorageEnabled = true
        settings.databaseEnabled = false
        settings.mediaPlaybackRequiresUserGesture = false
        @Suppress("DEPRECATION")
        settings.saveFormData = false
        settings.textZoom = 100
        settings.userAgentString = MOBILE_USER_AGENT

        webViewClient = SecureWebViewClient(
            deterministicBlocker = deterministicBlocker,
            mlClassifier = mlClassifier,
            onPageFinishedCallback = {
                if (isCosmeticAdHidingEnabled) {
                    evaluateJavascript(COSMETIC_AD_BLOCK_SCRIPT, null)
                }
            },
            onMediaDetected = { url, mime ->
                onMediaDetectedListener?.invoke(url, mime)
            },
            onUrlLoading = { url ->
                onUrlLoadingListener?.invoke(url)
            }
        )

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressChanged?.invoke(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                title?.let { onTitleReceived?.invoke(it) }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
                if (view != null && callback != null) {
                    onShowCustomViewListener?.invoke(view, callback)
                }
            }

            override fun onHideCustomView() {
                super.onHideCustomView()
                onHideCustomViewListener?.invoke()
            }
        }

        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            onDownloadTriggered?.invoke(url, userAgent, contentDisposition, mimetype, contentLength)
        }

        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
        clearCache(true)
        CookieManager.getInstance().removeAllCookies(null)
    }

    fun setJavaScriptEnabled(enabled: Boolean) {
        settings.javaScriptEnabled = enabled
    }

    fun setBlockPopups(block: Boolean) {
        settings.javaScriptCanOpenWindowsAutomatically = !block
    }

    fun setTextZoomLevel(zoom: Int) {
        settings.textZoom = zoom
    }

    fun setDesktopMode(enabled: Boolean) {
        settings.userAgentString = if (enabled) DESKTOP_USER_AGENT else MOBILE_USER_AGENT
        settings.useWideViewPort = enabled
        settings.loadWithOverviewMode = enabled
    }

    fun setQubeId(qubeId: String) {
        this.qubeId = qubeId
    }

    fun getQubeId(): String = qubeId

    fun clearAllData() {
        clearCache(true)
        clearHistory()
        CookieManager.getInstance().removeAllCookies(null)
    }

    class SecureWebViewClient(
        private val deterministicBlocker: DeterministicBlocker,
        private val mlClassifier: MLClassifier,
        private val onPageFinishedCallback: () -> Unit,
        private val onMediaDetected: (String, String) -> Unit,
        private val onUrlLoading: (String) -> Unit
    ) : WebViewClient() {

        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if (url != null) {
                onUrlLoading(url)
                view?.loadUrl(url)
            }
            return true
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val url = request?.url?.toString() ?: return null

            if (runBlocking { deterministicBlocker.isBlocked(url) }) {
                return createBlockedResponse()
            }

            if (mlClassifier.isModelLoaded() && runBlocking { mlClassifier.isBlocked(url) }) {
                return createBlockedResponse()
            }

            // Media URL detector (.mp4, .webm, .mkv, .mp3, .m4a)
            val lowerUrl = url.lowercase()
            if (lowerUrl.contains(".mp4") || lowerUrl.contains(".webm") || lowerUrl.contains(".m3u8") || lowerUrl.contains(".m4a")) {
                val mime = when {
                    lowerUrl.contains(".mp4") -> "video/mp4"
                    lowerUrl.contains(".webm") -> "video/webm"
                    lowerUrl.contains(".m4a") || lowerUrl.contains(".mp3") -> "audio/mpeg"
                    else -> "video/any"
                }
                onMediaDetected(url, mime)
            }

            return null
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onPageFinishedCallback()
        }

        private fun createBlockedResponse(): WebResourceResponse =
            WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), null)
    }
}
