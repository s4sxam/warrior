package com.tanay.warrior.nnn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tanay.warrior.ui.theme.BgBlack
import com.tanay.warrior.ui.theme.LocalAccentColor

private enum class NnnRoute { SETUP, COMMAND, ACTIVITY, INSIGHTS, SETTINGS, BROWSER, FOCUS }

/**
 * Entry point of the whole NNN feature: a full-screen overlay (same pattern as Achievements) with its own
 * small navigation stack. Everything it shows comes from [NnnViewModel]; it never reads habit state.
 */
@Composable
fun NnnRoot(vm: NnnViewModel, onClose: () -> Unit) {
    val ui by vm.ui.collectAsState()
    var route by remember { mutableStateOf<NnnRoute?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.messages.collect { m ->
            if (m is NnnMessage.Text) snackbar.showSnackbar(if (m.body.isBlank()) m.title else "${m.title} — ${m.body}")
        }
    }

    val current = route ?: if (!ui.loaded) null else if (ui.needsSetup) NnnRoute.SETUP else NnnRoute.COMMAND

    // Sub-screens go back to the command center; the command center / setup close the overlay.
    BackHandler(enabled = current != null && current != NnnRoute.SETUP) {
        if (current == NnnRoute.COMMAND) onClose() else route = NnnRoute.COMMAND
    }

    Box(Modifier.fillMaxSize().background(BgBlack).statusBarsPadding().navigationBarsPadding().imePadding()) {
        when (current) {
            null -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = LocalAccentColor.current)
            NnnRoute.SETUP -> NnnSetupScreen(
                ui = ui, onClose = onClose,
                onStart = { rules, settings, sb -> vm.start(rules, settings, sb) { route = NnnRoute.COMMAND } }
            )
            NnnRoute.COMMAND -> NnnCommandCenterScreen(
                ui = ui, onBack = onClose,
                onCheckIn = { vm.checkInToday() }, onReportSetback = { vm.reportSetback() },
                onOpenActivity = { route = NnnRoute.ACTIVITY }, onOpenInsights = { route = NnnRoute.INSIGHTS },
                onOpenSettings = { route = NnnRoute.SETTINGS }, onOpenBrowser = { route = NnnRoute.BROWSER },
                onOpenFocus = { route = NnnRoute.FOCUS }
            )
            NnnRoute.ACTIVITY -> NnnActivityScreen(ui, onBack = { route = NnnRoute.COMMAND }) { id, d, r -> vm.review(id, d, r) }
            NnnRoute.INSIGHTS -> NnnInsightsScreen(ui, onBack = { route = NnnRoute.COMMAND })
            NnnRoute.SETTINGS -> NnnSettingsScreen(vm, ui, onBack = { route = NnnRoute.COMMAND }, onAfterReset = { route = null })
            NnnRoute.BROWSER -> SafeBrowserScreen(vm, ui, onBack = { route = NnnRoute.COMMAND }, onOpenFocus = { route = NnnRoute.FOCUS })
            NnnRoute.FOCUS -> NnnFocusScreen(ui.state.settings.focusMessage, onBack = { route = NnnRoute.COMMAND })
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(12.dp))
    }
}
