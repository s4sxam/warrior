package com.tanay.warrior.ui.screens

// ─────────────────────────────────────────────────────────────────
// Modals.kt  — v6.0.0 (Redesign)
//
// CHANGES:
//   • ConfettiOverlay: removed clutter, single clear message
//   • RelapseModal: calmer tone, no troll shaming (drives drop-off)
//   • PanicModal: same action steps, cleaner layout
//   • All: standardized button heights, consistent border radius
// ─────────────────────────────────────────────────────────────────

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tanay.warrior.ui.theme.*

// ─────────────────────────────────────────────────────────────
// VICTORY OVERLAY — brief, positive, clears itself
// ─────────────────────────────────────────────────────────────
@Composable
fun ConfettiOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .then(
                // Tap anywhere to dismiss
                Modifier.then(
                    androidx.compose.ui.Modifier.clickable(
                        onClick = onDismiss,
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("✓", fontSize = 72.sp, color = VictoryGreen)
            Text(
                "VICTORY LOGGED",
                fontSize      = 26.sp,
                fontWeight    = FontWeight.Black,
                color         = VictoryGreen,
                letterSpacing = 3.sp,
            )
            Text(
                "One more brick in the wall.",
                fontSize = 14.sp,
                color    = TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Tap to continue",
                fontSize = 11.sp,
                color    = TextTertiary,
                letterSpacing = 1.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// RELAPSE MODAL — direct, accountable, not punishing
// ─────────────────────────────────────────────────────────────
@Composable
fun RelapseModal(
    trollMessage: String,           // kept in signature for compat, not displayed
    onDismiss: () -> Unit,
    onConfess: (String) -> Boolean,
) {
    var siteInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.5.dp, WarriorRed.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "LOG RELAPSE",
                fontSize      = 16.sp,
                fontWeight    = FontWeight.Black,
                color         = WarriorRed,
                letterSpacing = 2.sp,
            )

            Text(
                "Accountability is the first step. What triggered you?",
                fontSize   = 13.sp,
                color      = TextSecondary,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp,
            )

            OutlinedTextField(
                value         = siteInput,
                onValueChange = { siteInput = it },
                placeholder   = { Text("Site, app, or situation (optional)", color = TextTertiary, fontSize = 13.sp) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedTextColor      = TextPrimary,
                    unfocusedTextColor    = TextPrimary,
                    focusedBorderColor    = WarriorRed,
                    unfocusedBorderColor  = BorderColor,
                    cursorColor           = WarriorRed,
                ),
            )

            Button(
                onClick  = { onConfess(siteInput.ifBlank { "unknown" }) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = WarriorRed,
                    contentColor   = Color.White,
                ),
            ) {
                Text("CONFIRM & RESTART", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }

            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextTertiary, fontSize = 13.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PANIC MODAL — full screen, immediate action steps
// ─────────────────────────────────────────────────────────────
@Composable
fun PanicModal(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier         = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier            = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    "🚨  HOLD THE LINE",
                    fontSize      = 26.sp,
                    fontWeight    = FontWeight.Black,
                    color         = WarriorRed,
                    letterSpacing = 2.sp,
                )

                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PanicStep(number = "1", text = "Put the phone face-down")
                    PanicStep(number = "2", text = "Do 30 push-ups RIGHT NOW")
                    PanicStep(number = "3", text = "Drink a full glass of water")
                    PanicStep(number = "4", text = "The urge lasts 10–15 minutes")
                    PanicStep(number = "5", text = "You can outlast it")
                }

                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = VictoryGreen,
                        contentColor   = Color.Black,
                    ),
                ) {
                    Text(
                        "I AM IN CONTROL",
                        fontWeight    = FontWeight.Black,
                        fontSize      = 17.sp,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PanicStep(number: String, text: String) {
    Row(
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier         = Modifier
                .size(28.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(WarriorRed.copy(alpha = 0.15f))
                .border(1.dp, WarriorRed.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, fontSize = 12.sp, fontWeight = FontWeight.Black, color = WarriorRed)
        }
        Text(text, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
