package com.tanay.warrior2026

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            Warrior2026Theme {
                val state        by viewModel.state.collectAsStateWithLifecycle()
                val showConfetti by viewModel.showConfetti.collectAsStateWithLifecycle()

                if (!state.hasCompletedOnboarding) {
                    OnboardingScreen(onFinish = { viewModel.completeOnboarding() })
                } else {
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
                    )
                }
            }
        }
    }
}

// ── Nav items ─────────────────────────────────────────────────
data class NavItem(val view: ViewState, val label: String, val icon: ImageVector)

val navItems = listOf(
    NavItem(ViewState.DASHBOARD, "War Room",  Icons.Filled.Home),
    NavItem(ViewState.ANALYSIS,  "Analysis",  Icons.Filled.BarChart),
    NavItem(ViewState.ARCHIVE,   "Archives",  Icons.Filled.CalendarMonth),
    NavItem(ViewState.ABOUT,     "Code",      Icons.Filled.Shield),
)

// ── Root App with Framer-Motion Style Dock ────────────────────
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
) {
    var currentView       by remember { mutableStateOf(ViewState.DASHBOARD) }
    var showRelapseModal  by remember { mutableStateOf(false) }
    var showPanicModal    by remember { mutableStateOf(false) }
    var showAlreadySnack  by remember { mutableStateOf(false) }
    var trollMessage      by remember { mutableStateOf("") }

    // Dock State for Magnification (Local X coordinate)
    var fingerX by remember { mutableStateOf(-1f) }

    BackHandler {
        when {
            showRelapseModal -> showRelapseModal = false
            showPanicModal   -> showPanicModal   = false
            currentView != ViewState.DASHBOARD -> currentView = ViewState.DASHBOARD
        }
    }

    LaunchedEffect(showAlreadySnack) {
        if (showAlreadySnack) { kotlinx.coroutines.delay(2500); showAlreadySnack = false }
    }

    Scaffold(
        containerColor = BgBlack,
        bottomBar = {
            WarriorMagnifiedDock(
                items = navItems,
                current = currentView,
                fingerX = fingerX,
                onFingerMove = { fingerX = it },
                onSelect = { currentView = it }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("COMMANDER MODE", fontSize = 10.sp, color = TextTertiary, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                        Text("WARRIOR 2026", fontSize = 22.sp, fontWeight = FontWeight.Black, color = WarriorRed)
                    }
                    if (state.isTodayLogged()) {
                        IconButton(onClick = onUndoToday) {
                            Icon(Icons.Filled.Undo, contentDescription = "Undo", tint = TextTertiary, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                // Screen content
                AnimatedContent(
                    targetState = currentView,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                    label = "screen",
                    modifier = Modifier.weight(1f)
                ) { view ->
                    when (view) {
                        ViewState.DASHBOARD -> DashboardScreen(
                            state = state,
                            onPanicClick = { showPanicModal = true },
                            onVictoryClick = {
                                if (state.isTodayLogged() && state.history[com.tanay.warrior2026.data.todayKey()]?.status == "clean") showAlreadySnack = true
                                else onLogVictory()
                            },
                            onRelapseClick = {
                                trollMessage = trollMessages.random()
                                showRelapseModal = true
                            }
                        )
                        ViewState.ANALYSIS -> AnalysisScreen(state = state)
                        ViewState.ARCHIVE  -> ArchiveScreen(state = state)
                        ViewState.ABOUT    -> AboutScreen(onExport = onExport, onImport = onImport)
                    }
                }
            }

            // UI Overlays
            if (showConfetti) {
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(2000); onClearConfetti() }
                ConfettiOverlay(onDismiss = onClearConfetti)
            }
            if (showRelapseModal) {
                RelapseModal(trollMessage = trollMessage, onDismiss = { showRelapseModal = false }, onConfess = { url ->
                    val ok = onLogRelapse(url)
                    if (ok) showRelapseModal = false
                    ok
                })
            }
            if (showPanicModal) PanicModal(onDismiss = { showPanicModal = false })
        }
    }
}

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
            .padding(bottom = 20.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .height(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0D0D0D).copy(alpha = 0.98f))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { onFingerMove(-1f) },
                        onDragCancel = { onFingerMove(-1f) },
                        onDrag = { change, _ ->
                            // Use local position relative to the Row
                            onFingerMove(change.position.x)
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEach { item ->
                MagnifiedDockItem(
                    item = item,
                    isSelected = current == item.view,
                    fingerX = fingerX,
                    onClick = { onSelect(item.view) }
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
    
    // Calculate distance based on local Row coordinates
    val distance = if (fingerX == -1f) Float.MAX_VALUE else abs(fingerX - itemCenterX)
    val magnification = 0.4f 
    val range = 250f        
    
    val targetScale = if (distance < range) {
        1f + (magnification * (1f - (distance / range)))
    } else {
        1f
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                // Center relative to parent (the Row)
                itemCenterX = coords.positionInParent().x + (coords.size.width / 2)
            }
            .scale(scale)
            .width(52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color(0xFF1A0000) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (isSelected) WarriorRed else TextTertiary,
                modifier = Modifier.size(24.dp)
            )
        }
        if (scale > 1.1f || isSelected) {
            Text(
                text = item.label,
                fontSize = 8.sp,
                color = if (isSelected) WarriorRed else TextTertiary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}