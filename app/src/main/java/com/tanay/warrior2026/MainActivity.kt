package com.tanay.warrior2026

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tanay.warrior2026.data.ViewState
import com.tanay.warrior2026.ui.screens.*
import com.tanay.warrior2026.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: WarriorViewModel by viewModels()

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — scheduler already set up */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            Warrior2026Theme {
                WarriorApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun WarriorApp(viewModel: WarriorViewModel) {
    val state by viewModel.state.collectAsState()
    val showConfetti by viewModel.showConfetti.collectAsState()

    var currentView by remember { mutableStateOf(ViewState.DASHBOARD) }
    var sidebarOpen by remember { mutableStateOf(false) }
    var showRelapseModal by remember { mutableStateOf(false) }
    var showPanicModal by remember { mutableStateOf(false) }
    var showAlreadyLoggedSnack by remember { mutableStateOf(false) }
    var trollMessage by remember { mutableStateOf("") }

    LaunchedEffect(showAlreadyLoggedSnack) {
        if (showAlreadyLoggedSnack) {
            kotlinx.coroutines.delay(2500)
            showAlreadyLoggedSnack = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "COMMANDER MODE",
                        fontSize = 8.sp,
                        color = Color(0xFF444444),
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp
                    )
                    Text(
                        "WARRIOR 2026",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        style = LocalTextStyle.current.copy(
                            brush = Brush.horizontalGradient(listOf(WarriorRed, DarkRed))
                        )
                    )
                }
                IconButton(onClick = { sidebarOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Color(0xFF444444), modifier = Modifier.size(28.dp))
                }
            }

            // Content
            AnimatedContent(
                targetState = currentView,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                },
                label = "view_transition",
                modifier = Modifier.weight(1f)
            ) { view ->
                when (view) {
                    ViewState.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        state = state,
                        onPanicClick = { showPanicModal = true },
                        onVictoryClick = {
                            if (viewModel.isTodayLogged()) {
                                showAlreadyLoggedSnack = true
                            } else {
                                viewModel.logVictory()
                            }
                        },
                        onRelapseClick = {
                            trollMessage = viewModel.trollMessages.random()
                            showRelapseModal = true
                        }
                    )
                    ViewState.ANALYSIS -> AnalysisScreen(state = state)
                    ViewState.ARCHIVE  -> ArchiveScreen(state = state)
                    ViewState.ABOUT    -> AboutScreen()
                }
            }
        }

        // Already logged snackbar
        AnimatedVisibility(
            visible = showAlreadyLoggedSnack,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(VictoryGreen)
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text("Battle already reported today. ✅", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
        }

        // Confetti overlay (simple visual feedback)
        if (showConfetti) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                viewModel.clearConfetti()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VictoryGreen.copy(alpha = 0.06f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { viewModel.clearConfetti() }
            )
        }

        // Sidebar
        AnimatedVisibility(
            visible = sidebarOpen,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(250))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.98f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    listOf(
                        "BACK TO WAR" to ViewState.DASHBOARD,
                        "CURRENT ANALYSIS" to ViewState.ANALYSIS,
                        "MONTHLY ARCHIVES" to ViewState.ARCHIVE,
                        "ABOUT" to ViewState.ABOUT,
                    ).forEach { (label, view) ->
                        Text(
                            label,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (currentView == view) WarriorRed else Color(0xFF444444),
                            letterSpacing = 2.sp,
                            modifier = Modifier
                                .padding(vertical = 20.dp)
                                .clickable {
                                    currentView = view
                                    sidebarOpen = false
                                }
                        )
                    }
                    Spacer(Modifier.height(30.dp))
                    IconButton(
                        onClick = { sidebarOpen = false },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = WarriorRed, modifier = Modifier.size(36.dp))
                    }
                }
            }
        }

        // Relapse Modal
        if (showRelapseModal) {
            var urlInput by remember { mutableStateOf("") }
            var urlError by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.99f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(25.dp)
                        .clip(RoundedCornerShape(35.dp))
                        .background(Color(0xFF0A0A0A))
                        .padding(40.dp, 35.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(trollMessage, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center, lineHeight = 30.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("PASTE THE URL OF YOUR DEFEAT", fontSize = 10.sp, color = Color(0xFF555555), fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it; urlError = false },
                        placeholder = { Text("https://shameful-site.com", color = Color(0xFF333333), fontSize = 13.sp) },
                        isError = urlError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF111111),
                            unfocusedContainerColor = Color(0xFF111111),
                            focusedBorderColor = WarriorRed,
                            unfocusedBorderColor = Color(0xFF222222),
                        ),
                        shape = RoundedCornerShape(15.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (urlError) {
                        Text("Enter a valid URL, loser.", fontSize = 11.sp, color = WarriorRed, fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val success = viewModel.logRelapse(urlInput)
                            if (success) showRelapseModal = false
                            else urlError = true
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VictoryGreen)
                    ) {
                        Text("CONFESS & RESET", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black)
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showRelapseModal = false }) {
                        Text("CANCEL", fontSize = 11.sp, color = Color(0xFF333333), fontWeight = FontWeight.ExtraBold)
                    }

                    Text("DON'T LIE TO THE SYSTEM.", fontSize = 10.sp, color = Color(0xFF444444), fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }
        }

        // Panic Modal
        if (showPanicModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.99f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(25.dp)
                        .clip(RoundedCornerShape(35.dp))
                        .background(Color(0xFF0A0A0A))
                        .padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💀", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("EMERGENCY PROTOCOL", fontSize = 16.sp, fontWeight = FontWeight.Black, color = WarriorRed, letterSpacing = 2.sp)
                    Spacer(Modifier.height(20.dp))

                    listOf(
                        "DO 50 PUSHUPS NOW",
                        "COLD SHOWER. 3 MINUTES.",
                        "CALL SOMEONE. RIGHT NOW.",
                        "GO OUTSIDE. WALK.",
                        "DRINK A FULL GLASS OF WATER",
                    ).forEachIndexed { i, cmd ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0xFF111111))
                                .padding(16.dp, 14.dp)
                        ) {
                            Text(
                                "${i + 1}. $cmd",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(
                        "DON'T BE A SLAVE TO CHEAP PIXELS.",
                        fontSize = 11.sp,
                        color = Color(0xFF444444),
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showPanicModal = false },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = WarriorRed)
                    ) {
                        Text("I AM IN CONTROL", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
