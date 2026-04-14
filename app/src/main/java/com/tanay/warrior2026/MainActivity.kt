package com.tanay.warrior2026

// [UPDATE] v2.0.0: Added Commander Profile flow, Leaderboard tab, bot state wiring

import android.Manifest
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        setContent {
            Warrior2026Theme {
                val state             by viewModel.state.collectAsStateWithLifecycle()
                val showConfetti      by viewModel.showConfetti.collectAsStateWithLifecycle()
                val bots              by viewModel.bots.collectAsStateWithLifecycle()
                val isGeneratingBots  by viewModel.isGeneratingBots.collectAsStateWithLifecycle()

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
                        WarriorApp(
                            state           = state,
                            showConfetti    = showConfetti,
                            onLogVictory    = { viewModel.logVictory() },
                            onLogRelapse    = { url -> viewModel.logRelapse(url) },
                            onUndoToday     = { viewModel.undoToday() },
                            onClearConfetti = { viewModel.clearConfetti() },
                            onExport        = { viewModel.exportJson() },
                            onImport        = { json -> viewModel.importJson(json) },
                            trollMessages   = viewModel.trollMessages,
                            regionalBoard   = viewModel.regionalLeaderboard(),
                            globalBoard     = viewModel.globalLeaderboard(),
                            getBotProfile   = { id -> viewModel.getBotProfile(id) }
                        )
                    }
                }
            }
        }
    }
}

// ── Nav items ──────────────────────────────────────────────────────────────────
data class NavItem(val view: ViewState, val label: String, val icon: ImageVector)

val navItems = listOf(
    NavItem(ViewState.DASHBOARD,   "War Room",    Icons.Filled.Home),
    NavItem(ViewState.LEADERBOARD, "Arena",       Icons.Filled.EmojiEvents),
    NavItem(ViewState.ANALYSIS,    "Analysis",    Icons.Filled.BarChart),
    NavItem(ViewState.ARCHIVE,     "Archives",    Icons.Filled.CalendarMonth),
    NavItem(ViewState.ABOUT,       "Code",        Icons.Filled.Shield),
)

// ── Root App ───────────────────────────────────────────────────────────────────
@Composable
fun WarriorApp(
    state: com.tanay.warrior2026.data.WarriorState,
    showConfetti: Boolean,
    onLogVictory: () -> Unit,
    onLogRelapse: (String) -> Boolean,
    onUndoToday: () -> Unit,
    onClearConfetti: () -> Unit,
    onExport: () -> String,
    onImport: (String) -> Boolean,
    trollMessages: List<String>,
    regionalBoard: List<com.tanay.warrior2026.data.BotSimulator.LeaderboardEntry>,
    globalBoard: List<com.tanay.warrior2026.data.BotSimulator.LeaderboardEntry>,
    getBotProfile: (Int) -> com.tanay.warrior2026.data.BotProfile?
) {
    var currentView       by remember { mutableStateOf(ViewState.DASHBOARD) }
    var showRelapseModal  by remember { mutableStateOf(false) }
    var showPanicModal    by remember { mutableStateOf(false) }
    var trollMessage      by remember { mutableStateOf("") }
    var fingerX           by remember { mutableStateOf(-1f) }

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
                items         = navItems,
                current       = currentView,
                fingerX       = fingerX,
                onFingerMove  = { fingerX = it },
                onSelect      = { currentView = it }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("COMMANDER MODE", fontSize = 10.sp, color = TextTertiary,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                        Text("WARRIOR 2026", fontSize = 22.sp,
                            fontWeight = FontWeight.Black, color = WarriorRed)
                    }
                    if (state.isTodayLogged()) {
                        IconButton(onClick = onUndoToday) {
                            Icon(Icons.Filled.Undo, contentDescription = "Undo",
                                tint = TextTertiary, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                AnimatedContent(
                    targetState  = currentView,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                    label        = "screen",
                    modifier     = Modifier.weight(1f)
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
                        ViewState.ABOUT    -> AboutScreen(onExport = onExport, onImport = onImport)
                    }
                }
            }

            if (showConfetti) {
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(2000); onClearConfetti() }
                ConfettiOverlay(onDismiss = onClearConfetti)
            }
            if (showRelapseModal) {
                RelapseModal(trollMessage = trollMessage,
                    onDismiss = { showRelapseModal = false },
                    onConfess = { url ->
                        val ok = onLogRelapse(url)
                        if (ok) showRelapseModal = false
                        ok
                    })
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
        targetValue  = targetScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label        = "scale"
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
                        if (item.view == ViewState.LEADERBOARD) Color(0xFF1A1400)
                        else Color(0xFF1A0000)
                    } else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
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
