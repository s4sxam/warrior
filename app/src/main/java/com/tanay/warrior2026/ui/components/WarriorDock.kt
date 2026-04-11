package com.tanay.warrior2026.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior2026.data.ViewState
import com.tanay.warrior2026.ui.theme.*
import kotlin.math.abs

@Composable
fun WarriorDock(
    current: ViewState,
    items: List<com.tanay.warrior2026.NavItem>,
    onSelect: (ViewState) -> Unit
) {
    // Track the horizontal position of the touch/finger
    var mouseX by remember { mutableStateOf(-1f) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .height(72.dp) // Height of the Dock container
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0D0D0D).copy(alpha = 0.95f))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            mouseX = offset.x
                            tryAwaitRelease()
                            mouseX = -1f // Reset when finger lifted
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEach { item ->
                DockItem(
                    item = item,
                    isSelected = current == item.view,
                    mouseX = mouseX,
                    onClick = { onSelect(item.view) }
                )
            }
        }
    }
}

@Composable
fun DockItem(
    item: com.tanay.warrior2026.NavItem,
    isSelected: Boolean,
    mouseX: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var itemCenterX by remember { mutableStateOf(0f) }
    
    // Magnification Logic: Calculate distance from touch to center of icon
    val distance = if (mouseX == -1f) Float.MAX_VALUE else abs(mouseX - itemCenterX)
    
    // Scale factor: 1.0 (normal) to 1.5 (magnified)
    val targetScale = if (distance < 200f) {
        1f + (0.5f * (1f - (distance / 200f)))
    } else {
        1f
    }
    
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 150f),
        label = "dock_scale"
    )

    Column(
        modifier = Modifier
            .onGloballyPositioned { layoutCoordinates ->
                itemCenterX = layoutCoordinates.positionInWindow().x + (layoutCoordinates.size.width / 2)
            }
            .scale(animatedScale)
            .width(50.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The Icon
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
        
        // The Label (Fades out when not selected/hovered to stay clean)
        Text(
            text = item.label,
            fontSize = 9.sp,
            color = if (isSelected) WarriorRed else TextTertiary,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
            maxLines = 1
        )
    }
}