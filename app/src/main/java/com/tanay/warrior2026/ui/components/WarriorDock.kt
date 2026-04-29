package com.tanay.warrior.ui.components

// ─────────────────────────────────────────────────────────────────
// WarriorDock.kt  — v6.0.0 (Redesign)
//
// CHANGES:
//   • Removed macOS-style magnification physics
//     Reason: caused jank on mid-range Android via continuous
//     positionInWindow() calls + spring animations per frame
//   • Replaced with clean press-scale (single spring, one item)
//   • Active item: filled background + red icon + label
//   • Inactive item: icon only, no label (declutters)
//   • Consistent 56dp touch target per WCAG / Material guidelines
//   • Bottom padding respects edge-to-edge window insets
// ─────────────────────────────────────────────────────────────────

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior.NavItem
import com.tanay.warrior.data.ViewState
import com.tanay.warrior.ui.theme.*

@Composable
fun WarriorDock(
    current:  ViewState,
    items:    List<NavItem>,
    onSelect: (ViewState) -> Unit,
) {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0C0C0C))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { item ->
                DockItem(
                    item       = item,
                    isSelected = current == item.view,
                    onClick    = { onSelect(item.view) },
                )
            }
        }
    }
}

@Composable
private fun DockItem(
    item:       NavItem,
    isSelected: Boolean,
    onClick:    () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()

    // Simple press-scale — one spring, no per-frame position tracking
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label         = "dock_scale",
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF1A0000) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector        = item.icon,
            contentDescription = item.label,
            tint               = if (isSelected) WarriorRed else TextTertiary,
            modifier           = Modifier.size(22.dp),
        )
        // Label only for selected tab — cleaner at-a-glance
        if (isSelected) {
            Spacer(Modifier.height(2.dp))
            Text(
                text          = item.label,
                fontSize      = 8.sp,
                color         = WarriorRed,
                fontWeight    = FontWeight.Black,
                letterSpacing = 0.5.sp,
                maxLines      = 1,
            )
        }
    }
}
