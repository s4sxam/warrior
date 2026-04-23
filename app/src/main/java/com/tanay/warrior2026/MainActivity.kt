package com.tanay.warrior2026

// [UPDATE] v2.0.0: Added Commander Profile flow, Leaderboard tab, bot state wiring
// [UPDATE] v2.1.0: Added in-app update checker dialog
// [UPDATE] v2.2.0: Update dialog now uses DownloadManager — no browser open
// [FIX]    v2.3.0: Removed onTestUpdate parameter from WarriorApp and AboutScreen.
//                  Auto-check on launch is the only update trigger — no manual button.

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tanay.warrior2026.data.ViewState
import com.tanay.warrior2026.ui.screens.*
import com.tanay.warrior2026.ui.theme.*
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private val viewModel: WarriorViewModel by viewModels()

    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // v2.2.0: Check for update once on launch
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
                    // Step 1: Original onboarding pages
                    !state.hasCompletedOnboarding -> {
                        OnboardingScreen(onFinish = { viewModel.completeOnboarding() })
                    }
                    // Step 2: v2.0.0 Commander Profile setup
                    !state.hasCompletedProfile -> {
                        CommanderProfileScreen(
                            isGeneratingBots = isGeneratingBots,
                            onComplete = { name, dob, region ->
                                viewModel.completeProfile(name, dob, region)
                            }
                        )
                    }
                    // Step 3: Main app
                    else -> {
                        val context = LocalContext.current

                        // v2.2.0: Update dialog — phases: IDLE → DOWNLOADING → READY / FAILED
                        if (updateState.hasUpdate && !updateState.dismissed) {
                            Dialog(onDismissRequest = {
                                // Only allow dismiss when not actively downloading
                                if (updateState.phase != WarriorViewModel.DownloadPhase.DOWNLOADING) {
                                    viewModel.dismissUpdate()
                                }
                            }) {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF1A0000))
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text          = "⚔️ NEW VERSION AVAILABLE",
                                        fontSize      = 11.sp,
                                        fontWeight    = FontWeight.ExtraBold,
                                        color         = WarriorRed,
                                        letterSpacing = 2.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text       = "v${updateState.latestVersion} is ready",
                                        fontSize   = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color      = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text       = "Your streak and all data will NOT be lost.\nJust install over the existing app.",
                                        fontSize   = 13.sp,
                                        color      = TextTertiary,
                                        textAlign  = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(22.dp))

                                    when (updateState.phase) {

                                        // ── Not yet started ──────────────────
                                        WarriorViewModel.DownloadPhase.IDLE -> {
                                            Button(
                                                onClick = { viewModel.downloadUpdate() },
                                                colors  = ButtonDefaults.buttonColors(
                                                    containerColor = WarriorRed
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text          = "DOWNLOAD UPDATE",
                                                    fontWeight    = FontWeight.ExtraBold,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            TextButton(onClick = { viewModel.dismissUpdate() }) {
                                                Text("Later", color = TextTertiary)
                                            }
                                        }

                                        // ── Downloading — show progress bar ──
                                        WarriorViewModel.DownloadPhase.DOWNLOADING -> {
                                            val fraction = updateState.progressFraction
                                            if (fraction < 0f) {
                                                LinearProgressIndicator(
                                                    modifier   = Modifier.fillMaxWidth(),
                                                    color      = WarriorRed,
                                                    trackColor = Color(0xFF3A0000)
                                                )
                                            } else {
                                                LinearProgressIndicator(
                                                    progress   = { fraction },
                                                    modifier   = Modifier.fillMaxWidth(),
                                                    color      = WarriorRed,
                                                    trackColor = Color(0xFF3A0000)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val mbDone  = updateState.progressBytes / 1_048_576f
                                            val mbTotal = updateState.totalBytes / 1_048_576f
                                            val label = if (updateState.totalBytes > 0)
                                                "%.1f / %.1f MB".format(mbDone, mbTotal)
                                            else
                                                "Downloading..."
                                            Text(
                                                text     = label,
                                                fontSize = 12.sp,
                                                color    = TextTertiary
                                            )
                                        }

                                        // ── Done — prompt to install ─────────
                                        WarriorViewModel.DownloadPhase.READY -> {
                                            Button(
                                                onClick = { viewModel.installApk(context) },
                                                colors  = ButtonDefaults.buttonColors(
                                                    containerColor = WarriorRed
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text          = "INSTALL NOW",
                                                    fontWeight    = FontWeight.ExtraBold,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            TextButton(onClick = { viewModel.dismissUpdate() }) {
                                                Text("Later", color = TextTertiary)
                                            }
                                        }

                                        // ── Download failed ──────────────────
                                        WarriorViewModel.DownloadPhase.FAILED -> {
                                            Text(
                                                text      = "Download failed. Check your connection.",
                                                fontSize  = 12.sp,
                                                color     = WarriorRed,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = { viewModel.retryDownload() },
                                                colors  = ButtonDefaults.buttonColors(
                                                    containerColor = WarriorRed
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text          = "RETRY",
                                                    fontWeight    = FontWeight.ExtraBold,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            TextButton(onClick = { viewModel.dismissUpdate() }) {
                                                Text("Later", color = TextTertiary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        WarriorApp(
                            state           = state,
                            showConfetti    = showConfetti,
                            onLogVictory    = { viewModel.logVictory() },
                            onLogRelapse    = { url -> viewModel.logRelapse(url) },
                            onClearConfetti = { viewModel.clearConfetti() },
                            onExport        = { viewModel.exportJson() },
                            onImport        = { json -> viewModel.importJson(json) },
                            onImportPlain   = { days -> viewModel.importPlainDays(days) },
                            trollMessages   = viewModel.trollMessages,
                            regionalBoard   = regionalBoard,
                            globalBoard     = globalBoard,
                            getBotProfile   = { id -> viewModel.getBotProfile(id) }
                        )
                    }
                }
            }
        }
    }
}

// ── Root App ───────────────────────────────────────────────────────────────────
@Composable
fun WarriorApp(
    state: com.tanay.warrior2026.data.WarriorState,
    showConfetti: Boolean,
    onLogVictory: () -> Unit,
    onLogRelapse: (String) -> Boolean,
    onClearConfetti: () -> Unit,
    onExport: () -> String,
    onImport: (String) -> Boolean,
    onImportPlain: (Map<String, com.tanay.warrior2026.data.DayData>) -> Boolean,
    trollMessages: List<String>,
    regionalBoard: List<com.tanay.warrior2026.data.BotSimulator.LeaderboardEntry>,
    globalBoard: List<com.tanay.warrior2026.data.BotSimulator.LeaderboardEntry>,
    getBotProfile: (Int) -> com.tanay.warrior2026.data.BotProfile?
) {
    var currentView      by remember { mutableStateOf(ViewState.DASHBOARD) }
    var showRelapseModal by remember { mutableStateOf(false) }
    var showPanicModal   by remember { mutableStateOf(false) }
    var trollMessage     by remember { mutableStateOf("") }
    var fingerX          by remember { mutableStateOf(-1f) }

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
            WarriorMagnifiedDock(
                items        = navItems,
                current      = currentView,
                fingerX      = fingerX,
                onFingerMove = { fingerX = it },
                onSelect     = { currentView = it }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("COMMANDER MODE", fontSize = 10.sp, color = TextTertiary,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                        Text("WARRIOR 2026", fontSize = 22.sp,
                            fontWeight = FontWeight.Black, color = WarriorRed)
                    }
                }

                AnimatedContent(
                    targetState    = currentView,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                    label          = "screen",
                    modifier       = Modifier.weight(1f)
                ) { view ->
                    when (view) {
                        ViewState.DASHBOARD -> DashboardScreen(
                            state          = state,
                            onPanicClick   = { showPanicModal = true },
                            onVictoryClick = { onLogVictory() },
                            onRelapseClick = {
                                trollMessage = trollMessages.random()
                                showRelapseModal = true
                            }
                        )
                        ViewState.LEADERBOARD -> LeaderboardScreen(
                            regionalBoard = regionalBoard,
                            globalBoard   = globalBoard,
                            userRegion    = state.userProfile.region,
                            getBotProfile = getBotProfile
                        )
                        ViewState.ANALYSIS -> AnalysisScreen(state = state)
                        ViewState.ARCHIVE  -> ArchiveScreen(state = state)
                        ViewState.ABOUT    -> AboutScreen(
                            onExport      = onExport,
                            onImport      = onImport,
                            onImportPlain = onImportPlain
                        )
                    }
                }
            }

            if (showConfetti) {
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(2000); onClearConfetti() }
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
                    }
                )
            }
            if (showPanicModal) PanicModal(onDismiss = { showPanicModal = false })
        }
    }
}

// ── Magnified Dock ─────────────────────────────────────────────────────────────
@Composable
fun WarriorMagnifiedDock(
    items: List<NavItem>,
    current: ViewState,
    fingerX: Float,
    onFingerMove: (Float) -> Unit,
    onSelect: (ViewState) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .height(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0D0D0D).copy(alpha = 0.98f))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd    = { onFingerMove(-1f) },
                        onDragCancel = { onFingerMove(-1f) },
                        onDrag       = { change, _ -> onFingerMove(change.position.x) }
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                MagnifiedDockItem(
                    item       = item,
                    isSelected = current == item.view,
                    fingerX    = fingerX,
                    onClick    = { onSelect(item.view) }
                )
            }
        }
    }
}

@Composable
fun MagnifiedDockItem(
    item: NavItem,
    isSelected: Boolean,
    fingerX: Float,
    onClick: () -> Unit
) {
    var itemCenterX by remember { mutableStateOf(0f) }
    val distance = if (fingerX == -1f) Float.MAX_VALUE else abs(fingerX - itemCenterX)

    val targetScale = if (distance < 250f) {
        1f + (0.4f * (1f - (distance / 250f)))
    } else 1f

    val scale by animateFloatAsState(
        targetValue   = targetScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label         = "scale"
    )

    Column(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                itemCenterX = coords.positionInParent().x + (coords.size.width / 2)
            }
            .scale(scale)
            .width(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        if (item.view == ViewState.LEADERBOARD) Color(0xFF001A2E)
                        else Color(0xFF1A0000)
                    } else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = item.icon,
                contentDescription = item.label,
                tint = when {
                    !isSelected -> TextTertiary
                    item.view == ViewState.LEADERBOARD -> Gold
                    else -> WarriorRed
                },
                modifier = Modifier.size(22.dp)
            )
        }
        if (scale > 1.1f || isSelected) {
            Text(
                text       = item.label,
                fontSize   = 8.sp,
                color      = when {
                    !isSelected -> TextTertiary
                    item.view == ViewState.LEADERBOARD -> Gold
                    else -> WarriorRed
                },
                fontWeight = FontWeight.Bold,
                maxLines   = 1
            )
        }
    }
}
