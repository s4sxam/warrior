package com.tanay.warrior.ui.components

// ──────────────────────────────────────────────────────────────────────────────
// Toast.kt  —  Kotlin/Compose equivalent of the TypeScript Toast system
//
// Matches the TS behaviour exactly:
//   • Triggered only on Clean (success) or Fail (destructive) button press
//   • Spring enter/exit animation (opacity + scale + y offset)
//   • Linear progress bar that fills over `duration` ms
//   • Close (X) button
//   • Six position variants (top-right default)
//   • Five variant styles: default, destructive, success, warning, info
// ──────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon

// ─── Data ────────────────────────────────────────────────────────────────────

enum class ToastPosition {
    TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT, TOP_CENTER, BOTTOM_CENTER
}

enum class ToastVariant {
    DEFAULT, DESTRUCTIVE, SUCCESS, WARNING, INFO
}

data class ToastData(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val description: String = "",
    val variant: ToastVariant = ToastVariant.DEFAULT,
    val duration: Long = 5000L
)

// ─── State holder (hoisted) ───────────────────────────────────────────────────

class ToastState {
    var current by mutableStateOf<ToastData?>(null)
        private set

    fun show(toast: ToastData) {
        current = toast
    }

    fun dismiss() {
        current = null
    }
}

@Composable
fun rememberToastState() = remember { ToastState() }

// ─── Colours per variant (mirrors TS toastVariants cva) ──────────────────────

private data class ToastColors(
    val background: Color,
    val border: Color,
    val text: Color,
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
        background  = Color(0xFFFFE5E5),  // red-100 equivalent
        border      = Color(0xFFEF4444),  // red-500
        text        = Color(0xFF991B1B),  // red-800
        progressBar = Color(0xFFDC2626)   // red-600
    )
    ToastVariant.SUCCESS     -> ToastColors(
        background  = Color(0xFFDCFCE7),  // green-100
        border      = Color(0xFF22C55E),  // green-500
        text        = Color(0xFF166534),  // green-800
        progressBar = Color(0xFF16A34A)   // green-600
    )
    ToastVariant.WARNING     -> ToastColors(
        background  = Color(0xFFFEF9C3),  // yellow-100
        border      = Color(0xFFEAB308),  // yellow-500
        text        = Color(0xFF854D0E),  // yellow-800
        progressBar = Color(0xFFCA8A04)   // yellow-600
    )
    ToastVariant.INFO        -> ToastColors(
        background  = Color(0xFFEFF6FF),  // blue-50
        border      = Color(0xFF3B82F6),  // blue-500
        text        = Color(0xFF1E40AF),  // blue-800
        progressBar = Color(0xFF2563EB)   // blue-600
    )
}

// ─── Single Toast card ────────────────────────────────────────────────────────

@Composable
private fun ToastCard(
    toast: ToastData,
    onClose: () -> Unit
) {
    val colors = toastColors(toast.variant)
    val shape  = RoundedCornerShape(12.dp)

    // Progress bar animation — fills from 0% to 100% over `duration`
    val progress = remember { Animatable(0f) }
    LaunchedEffect(toast.id) {
        progress.animateTo(
            targetValue  = 1f,
            animationSpec = tween(
                durationMillis = toast.duration.toInt(),
                easing         = LinearEasing
            )
        )
        onClose()  // auto-dismiss when bar completes
    }

    Box(
        modifier = Modifier
            .width(340.dp)
            .clip(shape)
            .background(colors.background)
            .border(1.dp, colors.border, shape)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 40.dp, bottom = 14.dp)) {
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

        // ── Close button ──
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick    = onClose
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

        // ── Progress bar ──
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

// ─── Viewport — overlay that renders the toast in the correct corner ──────────
//
// Usage: place ToastViewport() inside your root Box/Scaffold content
//        and pass the same ToastState you call .show() on.

@Composable
fun ToastViewport(
    state: ToastState,
    position: ToastPosition = ToastPosition.TOP_RIGHT,
    modifier: Modifier = Modifier
) {
    val toast = state.current

    val boxAlignment = when (position) {
        ToastPosition.TOP_RIGHT     -> Alignment.TopEnd
        ToastPosition.TOP_LEFT      -> Alignment.TopStart
        ToastPosition.TOP_CENTER    -> Alignment.TopCenter
        ToastPosition.BOTTOM_RIGHT  -> Alignment.BottomEnd
        ToastPosition.BOTTOM_LEFT   -> Alignment.BottomStart
        ToastPosition.BOTTOM_CENTER -> Alignment.BottomCenter
    }

    val isTop = position in listOf(
        ToastPosition.TOP_RIGHT, ToastPosition.TOP_LEFT, ToastPosition.TOP_CENTER
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .zIndex(100f),
        contentAlignment = boxAlignment
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
                ToastCard(toast = it, onClose = { state.dismiss() })
            }
        }
    }
}
