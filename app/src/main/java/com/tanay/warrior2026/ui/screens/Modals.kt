package com.tanay.warrior.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tanay.warrior.ui.theme.*

@Composable
fun ConfettiOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔥", fontSize = 80.sp)
            Text("VICTORY LOGGED", fontSize = 24.sp, fontWeight = FontWeight.Black, color = VictoryGreen)
            Text("ANOTHER BRICK IN THE WALL", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
fun RelapseModal(
    trollMessage: String,
    onDismiss: () -> Unit,
    onConfess: (String) -> Boolean
) {
    var siteInput by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBlack, RoundedCornerShape(24.dp))
                .border(2.dp, WarriorRed, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CONFESS WEAKNESS", color = WarriorRed, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Text(trollMessage, color = TextSecondary, textAlign = TextAlign.Center, fontSize = 14.sp)
                Spacer(Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = siteInput,
                    onValueChange = { siteInput = it },
                    placeholder = { Text("What triggered you? (site/app)", color = TextDim) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = WarriorRed,
                        unfocusedBorderColor = BorderColor
                    )
                )
                
                Spacer(Modifier.height(20.dp))
                
                Button(
                    onClick = { onConfess(siteInput.ifBlank { "unknown" }) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarriorRed)
                ) {
                    Text("CONFESS & RESTART", fontWeight = FontWeight.Black)
                }
                
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", color = TextTertiary)
                }
            }
        }
    }
}

@Composable
fun PanicModal(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ABORT MISSION", color = WarriorRed, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(20.dp))
                Text(
                    "DROP THE PHONE.\nDO 50 PUSHUPS.\nTAKE A COLD SHOWER.\n\nTHE URGE LASTS 15 MINUTES.\nYOU CAN OUTLAST IT.",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(40.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = VictoryGreen),
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Text("I AM IN CONTROL", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
                }
            }
        }
    }
}