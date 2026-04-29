package com.tanay.warrior

// ─────────────────────────────────────────────────────────────────
// MainActivity.kt  — v6.0.0 (Redesign)
//
// CHANGES:
//   • Removed WarriorMagnifiedDock + MagnifiedDockItem (per-frame jank)
//   • Uses new WarriorDock (simple press-scale, no drag tracking)
//   • Removed "progressive unlock" feature — confusing, not useful
//   • Removed top "COMMANDER MODE / WARRIOR 2026" header clutter
//   • Removed ARCHIVE tab from navItems (moved to About/backup)
//   • WarriorApp is clean: screens, modals, dock — nothing else
//   • Confetti auto-clears after 1800ms (was 2000ms, snappier)
//   • All CommanderVoice wiring preserved — it's a signature feature
//   • Update dialog preserved — same logic, cleaner layout
// ─────────────────────────────────────────────────────────────────

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tanay.warrior.data.BotProfile
import com.tanay.warrior.data.BotSimulator
import com.tanay.warrior.data.DayData
import com.tanay.warrior.data.ViewState
import com.tanay.warrior.data.WarriorState
import com.tanay.warrior.ui.components.CommanderVoice
import com.tanay.warrior.ui.components.WarriorDock
import com.tanay.warrior.ui.screens.*
import com.tanay.warrior.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: WarriorViewModel by viewModels()
    private lateinit var commander: CommanderVoice

    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        commander = CommanderVoice(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        viewModel.checkForUpdate(BuildConfig.VERSION_NAME)

        setContent {
            Warrior2026Theme {
                val state            by viewModel.state.collectAsStateWithLifecycle()
                val showConfetti     by viewModel.showConfetti.collectAsStateWithLifecycle()
                val isGeneratingBots by viewModel.isGeneratingBots.collectAsStateWithLifecycle()
                val updateState      by viewModel.updateState.collectAsStateWithLifecycle()
                val regionalBoard    by viewModel.regionalBoard.collectAsStateWithLifecycle()
                val globalBoard      by viewModel.globalBoard.collectAsStateWithLifecycle()

                when {
                    !state.hasCompletedOnboarding -> {
                        OnboardingScreen(onFinish = { viewModel.completeOnboarding() })
                    }
                    !state.hasCompletedProfile -> {
                        CommanderProfileScreen(
                            isGeneratingBots = isGeneratingBots,
                            onComplete = { name, dob, region ->
                                viewModel.completeProfile(name, dob, region)
                            }
                        )
                    }
                    else -> {
                        val context = LocalContext.current

                        // ── Update dialog ────────────────────────────────
                        if (updateState.hasUpdate && !updateState.dismissed) {
                            UpdateDialog(
                                updateState = updateState,
                                onDownload  = { viewModel.downloadUpdate() },
                                onInstall   = { viewModel.installApk(context) },
                                onRetry     = { viewModel.retryDownload() },
                                onDismiss   = { viewModel.dismissUpdate() },
                            )
                        }

                        WarriorApp(
                            state           = state,
                            vm              = viewModel,
                            showConfetti    = showConfetti,
                            onLogVictory    = {
                                viewModel.logVictory()
                                commander.speakVictory(state.streak + 1)
                            },
                            onLogRelapse    = { url ->
                                val ok = viewModel.logRelapse(url)
                                if (ok) commander.speakRelapse()
                                ok
                            },
                            onClearConfetti = { viewModel.clearConfetti() },
                            onExport        = { viewModel.exportJson() },
                            onImport        = { json -> viewModel.importJson(json) },
                            onImportPlain   = { days -> viewModel.importPlainDays(days) },
                            trollMessages   = viewModel.trollMessages,
                            regionalBoard   = regionalBoard,
                            globalBoard     = globalBoard,
                            getBotProfile   = { id -> viewModel.getBotProfile(id) },
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        commander.release()
    }
}

// ─────────────────────────────────────────────────────────────
// UPDATE DIALOG — extracted for readability
// ─────────────────────────────────────────────────────────────
@Composable
private fun UpdateDialog(
    updateState: WarriorViewModel.UpdateState,
    onDownload:  () -> Unit,
    onInstall:   (Context) -> Unit,
    onRetry:     () -> Unit,
    onDismiss:   () -> Unit,
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = {
        if (updateState.phase != WarriorViewModel.DownloadPhase.DOWNLOADING) onDismiss()
    }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "NEW VERSION AVAILABLE",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.ExtraBold,
                color         = WarriorRed,
                letterSpacing = 2.sp,
            )
            Text(
                "v${updateState.latestVersion}",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Black,
                color      = TextPrimary,
            )
            Text(
                "Your streak and data will NOT be lost.",
                fontSize  = 12.sp,
                color     = TextTertiary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(6.dp))

            when (updateState.phase) {
                WarriorViewModel.DownloadPhase.IDLE -> {
                    Button(
                        onClick  = onDownload,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = WarriorRed),
                    ) {
                        Text("DOWNLOAD UPDATE", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Later", color = TextTertiary)
                    }
                }
                WarriorViewModel.DownloadPhase.DOWNLOADING -> {
                    val fraction = updateState.progressFraction
                    if (fraction < 0f) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = WarriorRed, trackColor = Color(0xFF3A0000))
                    } else {
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth(), color = WarriorRed, trackColor = Color(0xFF3A0000))
                    }
                    val label = if (updateState.totalBytes > 0)
                        "%.1f / %.1f MB".format(updateState.progressBytes / 1_048_576f, updateState.totalBytes / 1_048_576f)
                    else "Downloading…"
                    Text(label, fontSize = 12.sp, color = TextTertiary)
                }
                WarriorViewModel.DownloadPhase.READY -> {
                    Button(
                        onClick  = { onInstall(context) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = VictoryGreen),
                    ) {
                        Text("INSTALL NOW", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Color.Black)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Later", color = TextTertiary)
                    }
                }
                WarriorViewModel.DownloadPhase.FAILED -> {
                    Text("Download failed. Check your connection.", fontSize = 12.sp, color = WarriorRed, textAlign = TextAlign.Center)
                    Button(
                        onClick  = onRetry,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = WarriorRed),
                    ) {
                        Text("RETRY", fontWeight = FontWeight.ExtraBold)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Later", color = TextTertiary)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WARRIOR APP — root composable
// ─────────────────────────────────────────────────────────────
@Composable
fun WarriorApp(
    state:           WarriorState,
    vm:              WarriorViewModel,
    showConfetti:    Boolean,
    onLogVictory:    () -> Unit,
    onLogRelapse:    (String) -> Boolean,
    onClearConfetti: () -> Unit,
    onExport:        () -> String,
    onImport:        (String) -> Boolean,
    onImportPlain:   (Map<String, DayData>) -> Boolean,
    trollMessages:   List<String>,
    regionalBoard:   List<BotSimulator.LeaderboardEntry>,
    globalBoard:     List<BotSimulator.LeaderboardEntry>,
    getBotProfile:   (Int) -> BotProfile?,
) {
    var currentView      by remember { mutableStateOf(ViewState.DASHBOARD) }
    var showRelapseModal by remember { mutableStateOf(false) }
    var showPanicModal   by remember { mutableStateOf(false) }
    var trollMessage     by remember { mutableStateOf("") }

    BackHandler {
        when {
            showRelapseModal -> showRelapseModal = false
            showPanicModal   -> showPanicModal   = false
            currentView != ViewState.DASHBOARD -> currentView = ViewState.DASHBOARD
        }
    }

    Scaffold(
        containerColor = BgBlack,
        bottomBar = {
            WarriorDock(
                current  = currentView,
                items    = navItems,
                onSelect = { currentView = it },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // ── Screen content ─────────────────────────────────
            AnimatedContent(
                targetState    = currentView,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(180)) },
                label          = "screen",
                modifier       = Modifier.fillMaxSize(),
            ) { view ->
                when (view) {
                    ViewState.DASHBOARD -> DashboardScreen(
                        state            = state,
                        onPanicClick     = { showPanicModal = true },
                        onVictoryClick   = { onLogVictory() },
                        onRelapseClick   = {
                            trollMessage = trollMessages.random()
                            showRelapseModal = true
                        },
                        onSaveConfession = { text -> vm.saveConfession(text) },
                    )
                    ViewState.LEADERBOARD -> LeaderboardScreen(
                        regionalBoard = regionalBoard,
                        globalBoard   = globalBoard,
                        userRegion    = state.userProfile.region,
                        getBotProfile = getBotProfile,
                        myStreak      = state.streak,
                        rivalStreak   = state.previousStreak,
                    )
                    ViewState.ANALYSIS -> AnalysisScreen(state = state)
                    ViewState.ARCHIVE  -> ArchiveScreen(state = state)
                    ViewState.HABITS   -> HabitsScreen(state = state, vm = vm)
                    ViewState.ABOUT    -> AboutScreen(
                        onExport      = onExport,
                        onImport      = onImport,
                        onImportPlain = onImportPlain,
                    )
                }
            }

            // ── Overlays ───────────────────────────────────────
            if (showConfetti) {
                LaunchedEffect(Unit) { delay(1800); onClearConfetti() }
                ConfettiOverlay(onDismiss = onClearConfetti)
            }
            if (showRelapseModal) {
                RelapseModal(
                    trollMessage = trollMessage,
                    onDismiss    = { showRelapseModal = false },
                    onConfess    = { url ->
                        val ok = onLogRelapse(url)
                        if (ok) showRelapseModal = false
                        ok
                    },
                )
            }
            if (showPanicModal) {
                PanicModal(onDismiss = { showPanicModal = false })
            }
        }
    }
}
