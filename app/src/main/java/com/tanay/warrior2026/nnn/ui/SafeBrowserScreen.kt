package com.tanay.warrior.nnn.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tanay.warrior.nnn.domain.InputResolution
import com.tanay.warrior.nnn.domain.NavigationDecision
import com.tanay.warrior.nnn.monitor.SafeBrowserClient
import com.tanay.warrior.ui.theme.*

/**
 * WARRIOR Safe Browser. It can see ONLY what is opened inside it. A direct visit to a domain that matches
 * the user's rule is stopped before it loads and handled by the NNN engine (recorded or not, per the user's rules).
 *
 * Hardening: no file/content access, no JavaScript bridges, no mixed content, no geolocation, no multiple
 * windows, Safe Browsing on, SSL errors cancel. URLs are never logged or stored.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SafeBrowserScreen(vm: NnnViewModel, ui: NnnUiState, onBack: () -> Unit, onOpenFocus: () -> Unit) {
    val monitoring = ui.state.monitorState(com.tanay.warrior.nnn.domain.MonitorSource.SAFE_BROWSER) ==
        com.tanay.warrior.nnn.domain.MonitorState.ACTIVE
    var webView by remember { mutableStateOf<WebView?>(null) }
    var address by remember { mutableStateOf("") }
    var blank by remember { mutableStateOf(true) }
    var blockedNotice by remember { mutableStateOf<Boolean?>(null) }  // null = no block; true/false = recorded or not
    var rendererGone by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current

    fun handleBlocked(host: String) {
        vm.onSafeBrowserBlocked(host) { recorded -> blockedNotice = recorded }
    }

    fun go(raw: String) {
        val policy = vm.navigationPolicy()
        val url = when (val r = policy.resolveInput(raw)) { is InputResolution.Navigate -> r.url; is InputResolution.Search -> r.url }
        when (val d = policy.evaluate(url, isMainFrame = true)) {
            NavigationDecision.Allow -> { blockedNotice = null; webView?.loadUrl(url) }
            is NavigationDecision.Block -> handleBlocked(d.host)
            else -> {}
        }
    }

    BackHandler { if (webView?.canGoBack() == true && blockedNotice == null) webView?.goBack() else onBack() }

    DisposableEffect(owner, webView) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) { Lifecycle.Event.ON_PAUSE -> webView?.onPause(); Lifecycle.Event.ON_RESUME -> webView?.onResume(); else -> {} }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    NnnPage("SAFE BROWSER", "Sees only what you open here", onBack) {
        if (!monitoring) {
            NnnBody("Safe Browser monitoring is off, so nothing opened here is checked or recorded. Turn it on in NNN settings to use Safe Browser with your challenge rules.", NnnAmber)
            Spacer(Modifier.height(8.dp))
        } else {
            NnnBody("Direct visits to sites matching your rule are stopped and handled by your NNN rules. Other browsers and apps are not covered.", TextTertiary)
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = address, onValueChange = { address = it }, singleLine = true, modifier = Modifier.weight(1f),
                label = { Text("Search or enter address") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go), keyboardActions = KeyboardActions(onGo = { go(address) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = LocalAccentColor.current,
                    focusedBorderColor = LocalAccentColor.current, unfocusedBorderColor = BorderColor,
                    focusedLabelColor = LocalAccentColor.current, unfocusedLabelColor = TextSecondary
                )
            )
            Spacer(Modifier.width(8.dp))
            NnnButton("GO", { go(address) }, enabled = monitoring && address.isNotBlank())
        }
        Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NnnButton("BACK", { webView?.goBack() }, primary = false, enabled = webView?.canGoBack() == true)
            NnnButton("RELOAD", { webView?.reload() }, primary = false, enabled = !blank)
            NnnButton("CLEAR DATA", { confirmClear = true }, primary = false)
        }
        Box(Modifier.fillMaxWidth().height(520.dp).clip(RoundedCornerShape(14.dp)).background(CardBlack)) {
            if (monitoring) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            with(settings) {
                                javaScriptEnabled = true            // most sites need it; there is NO JS bridge
                                domStorageEnabled = true
                                allowFileAccess = false
                                allowContentAccess = false
                                @Suppress("DEPRECATION") allowFileAccessFromFileURLs = false
                                @Suppress("DEPRECATION") allowUniversalAccessFromFileURLs = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                setGeolocationEnabled(false)
                                setSupportMultipleWindows(false)
                                javaScriptCanOpenWindowsAutomatically = false
                                safeBrowsingEnabled = true
                                builtInZoomControls = true; displayZoomControls = false
                            }
                            webViewClient = SafeBrowserClient(
                                policy = { vm.navigationPolicy() },
                                onBlocked = { host -> handleBlocked(host) },
                                onNavigationStarted = { isBlank -> blank = isBlank },
                                onRenderGone = { rendererGone = true }
                            )
                            webView = this
                        }
                    },
                    onRelease = { it.stopLoading(); it.destroy(); if (webView === it) webView = null }
                )
            }
            if (blank && blockedNotice == null && monitoring) {
                NnnBody("Type a website or a search above.", TextTertiary, Modifier.align(Alignment.Center).padding(24.dp))
            }
            if (rendererGone) {
                Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    NnnBody("The page stopped responding.", TextPrimary)
                    Spacer(Modifier.height(8.dp)); NnnButton("CLOSE BROWSER", onBack)
                }
            }
            blockedNotice?.let { recorded ->
                // Neutral, non-judgmental stop screen. Compose (not HTML) so there is no JS bridge into the app.
                Column(Modifier.fillMaxSize().background(CardBlack).padding(22.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Navigation stopped", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    NnnBody(
                        if (recorded) "That site matches a rule you chose for your NNN challenge, and your rule counts a direct visit. It was recorded — you can review it in Activity. The challenge continues."
                        else "That site matches a rule you chose for your NNN challenge. It wasn't recorded (your rules, an allowlist, or a recent identical event).",
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    NnnButton("OPEN FOCUS EXERCISE", onOpenFocus, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    NnnButton("LEAVE BROWSER", onBack, Modifier.fillMaxWidth(), primary = false)
                    Spacer(Modifier.height(8.dp))
                    NnnButton("STAY IN BROWSER", { blockedNotice = null }, Modifier.fillMaxWidth(), primary = false)
                }
            }
        }
    }

    if (confirmClear) {
        NnnConfirm("Clear browsing data?", "Removes this browser's cookies, cache and history. Your NNN records are not affected.", "CLEAR", {
            webView?.clearCache(true); webView?.clearHistory()
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
            blank = true
        }, { confirmClear = false })
    }
}
