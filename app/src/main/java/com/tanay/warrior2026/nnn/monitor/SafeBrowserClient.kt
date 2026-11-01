package com.tanay.warrior.nnn.monitor

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.tanay.warrior.nnn.domain.NavigationDecision
import com.tanay.warrior.nnn.domain.SafeBrowserNavigationPolicy
import java.io.ByteArrayInputStream

/**
 * Thin WebView shell around the unit-tested [SafeBrowserNavigationPolicy]. No decisions live here.
 *
 * Main-frame navigations are inspected in TWO places because Android does not route every kind of
 * navigation (e.g. some POSTs) through shouldOverrideUrlLoading: here and in shouldInterceptRequest.
 * Both may fire for one navigation — the engine's deduplicator records it once.
 *
 * Only the HOST of a blocked navigation leaves this class (in memory, to the engine). URLs are never
 * logged, stored or passed anywhere else. Sub-resources and sub-frames are never inspected.
 */
class SafeBrowserClient(
    private val policy: () -> SafeBrowserNavigationPolicy,
    private val onBlocked: (host: String) -> Unit,
    private val onNavigationStarted: (isBlank: Boolean) -> Unit,
    private val onRenderGone: () -> Unit
) : WebViewClient() {

    private val main = Handler(Looper.getMainLooper())

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        when (val d = policy().evaluate(request.url.toString(), request.isForMainFrame)) {
            NavigationDecision.Allow -> false
            is NavigationDecision.Block -> { view.stopLoading(); onBlocked(d.host); true }
            // Malformed or non-web schemes (javascript:, intent:, file: …) are never loaded in the WebView.
            NavigationDecision.InvalidAddress, is NavigationDecision.UnsupportedScheme -> true
        }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (!request.isForMainFrame) return null                       // never touch sub-resources
        val d = policy().evaluate(request.url.toString(), isMainFrame = true)
        if (d is NavigationDecision.Block) {
            main.post { view.stopLoading(); onBlocked(d.host) }       // this callback runs off the UI thread
            return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
        }
        return null
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        onNavigationStarted(url == null || url == "about:blank")
    }

    // Do NOT override onReceivedSslError: the default (cancel) is the safe behaviour.

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        onRenderGone()
        return true   // handled: keeps the app alive instead of crashing with the renderer
    }
}
