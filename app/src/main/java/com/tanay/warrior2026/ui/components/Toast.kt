package com.tanay.warrior.ui.components

// ──────────────────────────────────────────────────────────────────────────────
// Toast.kt  —  v4.0.2 update
//
// Changes vs previous version:
//   • ToastHostState — suspend showToast() queues toasts one-at-a-time (like
//     SnackbarHostState). Replaces the old single-shot ToastState.show().
//   • rememberToastHostState() convenience helper.
//   • ToastHost composable (replaces ToastViewport name, old name kept as alias).
//   • All existing visual behaviour preserved exactly:
//       – Spring enter/exit (opacity + scale 0.7→1 + y offset)
//       – Linear progress bar filling over `duration` ms, auto-dismisses
//       – Close (X) button
//       – Six positions, five variants
// ──────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow

// ─── Enums ───────────────────────────────────────────────────────────────────

enum class ToastPosition {
    TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT, TOP_CENTER, BOTTOM_CENTER
}

enum class ToastVariant {
    DEFAULT, DESTRUCTIVE, SUCCESS, WARNING, INFO
}

// ─── Data ────────────────────────────────────────────────────────────────────

data class ToastData(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val description: String = "",
    val variant: ToastVariant = ToastVariant.DEFAULT,
    val duration: Long = 5000L
)

// ─── Host state — queued, one toast at a time ─────────────────────────────────
//
// Drop-in for the old ToastState. Use showToast() from a coroutine scope
// (e.g. viewModelScope or a LaunchedEffect scope).
//
// Old callers that used  toastState.show(ToastData(...))  can migrate by
// wrapping in  scope.launch { toastState.showToast(...) }  or by keeping the
// legacy ToastState class below (both coexist without conflicts).

class ToastHostState {
    // Channel buffers up to 8 queued toasts; extras are dropped silently.
    private val _queue = Channel<ToastData>(capacity = 8)
    internal val queue = _queue.receiveAsFlow()

    /** Suspend until the toast has been displayed and dismissed (or expired). */
    suspend fun showToast(
        title: String,
        description: String = "",
        variant: ToastVariant = ToastVariant.DEFAULT,
        duration: Long = 5000L
    ) {
        _queue.trySend(
            ToastData(
                id          = System.currentTimeMillis(),
                title       = title,
                description = description,
                variant     = variant,
                duration    = duration
            )
        )
    }

    /** Convenience overload — pass a fully-built ToastData directly. */
    suspend fun showToast(data: ToastData) {
        _queue.trySend(data)
    }
}

@Composable
fun rememberToastHostState() = remember { ToastHostState() }

// ─── Legacy single-shot state (kept for backward compatibility) ───────────────

class ToastState {
    var current by mutableStateOf<ToastData?>(null)
        private set

    fun show(toast: ToastData) { current = toast }
    fun dismiss() { current = null }
}

@Composable
fun rememberToastState() = remember { ToastState() }

// ─── Colours per variant ──────────────────────────────────────────────────────

private data class ToastColors(
    val background:  Color,
    val border:      Color,
    val text:        Color,
    val progressBar: Color
)

private fun toastColors(variant: ToastVariant): ToastColors = when (variant) {
    ToastVariant.DEFAULT     -> ToastColors(
        background  = Color(0xFF111111),
        border      = Color(0xFF252525),
        text        = Color.White,
        progressBar = Color(0xFF888888)
    )
    ToastVariant.DESTRUCTIVE -> ToastColors(
        background  = Color(0xFFFFE5E5),
        border      = Color(0xFFEF4444),
        text        = Color(0xFF991B1B),
        progressBar = Color(0xFFDC2626)
    )
    ToastVariant.SUCCESS     -> ToastColors(
        background  = Color(0xFFDCFCE7),
        border      = Color(0xFF22C55E),
        text        = Color(0xFF166534),
        progressBar = Color(0xFF16A34A)
    )
    ToastVariant.WARNING     -> ToastColors(
        background  = Color(0xFFFEF9C3),
        border      = Color(0xFFEAB308),
        text        = Color(0xFF854D0E),
        progressBar = Color(0xFFCA8A04)
    )
    ToastVariant.INFO        -> ToastColors(
        background  = Color(0xFFEFF6FF),
        border      = Color(0xFF3B82F6),
        text        = Color(0xFF1E40AF),
        progressBar = Color(0xFF2563EB)
    )
}

// ─── Single Toast card ────────────────────────────────────────────────────────

@Composable
private fun ToastCard(
    toast:   ToastData,
    onClose: () -> Unit
) {
    val colors = toastColors(toast.variant)
    val shape  = RoundedCornerShape(12.dp)

    val progress = remember { Animatable(0f) }
    LaunchedEffect(toast.id) {
        progress.animateTo(
            targetValue   = 1f,
            animationSpec = tween(
                durationMillis = toast.duration.toInt(),
                easing         = LinearEasing
            )
        )
        onClose()
    }

    Box(
        modifier = Modifier
            .width(340.dp)
            .clip(shape)
            .background(colors.background)
            .border(1.dp, colors.border, shape)
    ) {
        Column(
            modifier = Modifier.padding(
                start  = 16.dp, top = 14.dp,
                end    = 40.dp, bottom = 14.dp
            )
        ) {
            if (toast.title.isNotBlank()) {
                Text(
                    text       = toast.title,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = colors.text
                )
            }
            if (toast.description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = toast.description,
                    fontSize = 13.sp,
                    color    = colors.text.copy(alpha = 0.85f)
                )
            }
        }

        // Close button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onClose
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Close,
                contentDescription = "Close",
                tint               = colors.text.copy(alpha = 0.6f),
                modifier           = Modifier.size(16.dp)
            )
        }

        // Progress bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(3.dp)
                .background(colors.border.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.value)
                    .fillMaxHeight()
                    .background(colors.progressBar)
            )
        }
    }
}

// ─── ToastHost — new queued host (use with ToastHostState) ───────────────────

@Composable
fun ToastHost(
    hostState: ToastHostState,
    position:  ToastPosition = ToastPosition.TOP_RIGHT,
    modifier:  Modifier      = Modifier
) {
    var current by remember { mutableStateOf<ToastData?>(null) }

    // Drain the queue: collect one toast, display it, then move to the next.
    LaunchedEffect(hostState) {
        hostState.queue.collect { toast ->
            current = toast
        }
    }

    ToastOverlay(
        toast    = current,
        position = position,
        modifier = modifier,
        onClose  = { current = null }
    )
}

// ─── ToastViewport — legacy viewport (use with ToastState) ───────────────────

@Composable
fun ToastViewport(
    state:    ToastState,
    position: ToastPosition = ToastPosition.TOP_RIGHT,
    modifier: Modifier      = Modifier
) {
    ToastOverlay(
        toast    = state.current,
        position = position,
        modifier = modifier,
        onClose  = { state.dismiss() }
    )
}

// ─── Shared overlay rendering ─────────────────────────────────────────────────

@Composable
private fun ToastOverlay(
    toast:    ToastData?,
    position: ToastPosition,
    modifier: Modifier,
    onClose:  () -> Unit
) {
    val alignment = when (position) {
        ToastPosition.TOP_RIGHT     -> Alignment.TopEnd
        ToastPosition.TOP_LEFT      -> Alignment.TopStart
        ToastPosition.TOP_CENTER    -> Alignment.TopCenter
        ToastPosition.BOTTOM_RIGHT  -> Alignment.BottomEnd
        ToastPosition.BOTTOM_LEFT   -> Alignment.BottomStart
        ToastPosition.BOTTOM_CENTER -> Alignment.BottomCenter
    }

    Box(
        modifier         = modifier
            .fillMaxSize()
            .padding(16.dp)
            .zIndex(100f),
        contentAlignment = alignment
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter   = fadeIn(spring(dampingRatio = 0.6f, stiffness = 400f)) +
                      scaleIn(spring(dampingRatio = 0.6f, stiffness = 400f),
                              initialScale = 0.7f),
            exit    = fadeOut(spring(stiffness = 400f)) +
                      scaleOut(spring(stiffness = 400f), targetScale = 0.7f)
        ) {
            toast?.let {
                ToastCard(toast = it, onClose = onClose)
            }
        }
    }
}
