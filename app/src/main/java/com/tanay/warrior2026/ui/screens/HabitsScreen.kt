
package com.tanay.warrior.ui.screens

// [NEW] v4.0.0: Multi-habit management screen.
// Shows all habits as cards; lets the user add, switch, rename, and delete habits.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior.WarriorViewModel
import com.tanay.warrior.data.Habit
import com.tanay.warrior.data.WarriorState

private val Red       = Color(0xFFCC0000)
private val DarkBg    = Color(0xFF0D0D0D)
private val CardBg    = Color(0xFF1A1A1A)
private val TextPri   = Color(0xFFEEEEEE)
private val TextSec   = Color(0xFF888888)
private val GreenDay  = Color(0xFF2ECC71)

@Composable
fun HabitsScreen(
    state: WarriorState,
    vm: WarriorViewModel
) {
    var showAddDialog    by remember { mutableStateOf(false) }
    var editHabit        by remember { mutableStateOf<Habit?>(null) }
    var confirmDeleteId  by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YOUR HABITS",
                    color = Red,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add habit",
                        tint = Red,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = "Tap a habit to make it active. Long-press to edit.",
                color = TextSec,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
            )

            // ── Habit list ────────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.habits, key = { it.id }) { habit ->
                    HabitCard(
                        habit      = habit,
                        isActive   = habit.id == state.activeHabitId,
                        onSelect   = { vm.switchHabit(habit.id) },
                        onEdit     = { editHabit = habit },
                        onDelete   = { confirmDeleteId = habit.id },
                        canDelete  = state.habits.size > 1
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // dock clearance
                }
            }
        }
    }

    // ── Add habit dialog ──────────────────────────────────────────────────────
    if (showAddDialog) {
        HabitEditDialog(
            title      = "NEW HABIT",
            initial    = Habit(id = "", name = "", emoji = "🔥"),
            onConfirm  = { name, emoji ->
                vm.addHabit(name, emoji)
                showAddDialog = false
            },
            onDismiss  = { showAddDialog = false }
        )
    }

    // ── Edit habit dialog ─────────────────────────────────────────────────────
    editHabit?.let { habit ->
        HabitEditDialog(
            title      = "EDIT HABIT",
            initial    = habit,
            onConfirm  = { name, emoji ->
                vm.renameHabit(habit.id, name, emoji)
                editHabit = null
            },
            onDismiss  = { editHabit = null }
        )
    }

    // ── Delete confirmation ───────────────────────────────────────────────────
    confirmDeleteId?.let { id ->
        val habit = state.habits.find { it.id == id }
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            containerColor   = CardBg,
            title = {
                Text("DELETE HABIT", color = Red, fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp, fontSize = 16.sp)
            },
            text = {
                Text(
                    "Delete \"${habit?.name}\"? All history for this habit will be lost forever.",
                    color = TextPri, fontSize = 14.sp, lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteHabit(id)
                    confirmDeleteId = null
                }) {
                    Text("DELETE", color = Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) {
                    Text("CANCEL", color = TextSec)
                }
            }
        )
    }
}

// ── Habit Card ────────────────────────────────────────────────────────────────

@Composable
private fun HabitCard(
    habit: Habit,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val borderColor = if (isActive) Red else Color(0xFF2A2A2A)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(width = if (isActive) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Emoji
        Text(text = habit.emoji, fontSize = 28.sp,
             modifier = Modifier.padding(end = 14.dp))

        // Name + stats
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                color = if (isActive) TextPri else TextSec,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip("🔥 ${habit.streak}d")
                StatChip("✅ ${habit.totalClean}")
                StatChip("Best ${habit.bestStreak}d")
            }
        }

        // Active indicator
        if (isActive) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Active",
                tint = GreenDay,
                modifier = Modifier.size(20.dp).padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Edit
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit",
                 tint = TextSec, modifier = Modifier.size(16.dp))
        }

        // Delete
        if (canDelete) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete",
                     tint = Color(0xFF552222), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun StatChip(text: String) {
    Text(text = text, color = TextSec, fontSize = 11.sp)
}

// ── Add / Edit dialog ─────────────────────────────────────────────────────────

private val EMOJI_OPTIONS = listOf(
    "🔥","💪","🧠","❤️","🚭","🍺","📵","💊","🎮","🍔","😴","🏃","📖","✍️","🧘"
)

@Composable
private fun HabitEditDialog(
    title: String,
    initial: Habit,
    onConfirm: (name: String, emoji: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name  by remember { mutableStateOf(initial.name) }
    var emoji by remember { mutableStateOf(initial.emoji) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = CardBg,
        title = {
            Text(title, color = Red, fontWeight = FontWeight.Black,
                letterSpacing = 2.sp, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit name", color = TextSec) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Red,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor     = TextPri,
                        unfocusedTextColor   = TextPri,
                        cursorColor          = Red
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Emoji picker
                Text("Choose icon", color = TextSec, fontSize = 12.sp)
                @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                    modifier = Modifier.height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(EMOJI_OPTIONS.size) { i ->
                        val e = EMOJI_OPTIONS[i]
                        val selected = e == emoji
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Red.copy(alpha = 0.25f) else Color(0xFF222222))
                                .border(
                                    width = if (selected) 1.5.dp else 0.dp,
                                    color = if (selected) Red else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { emoji = e }
                        ) {
                            Text(e, fontSize = 20.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), emoji) },
                enabled = name.isNotBlank()
            ) {
                Text("SAVE", color = if (name.isNotBlank()) Red else TextSec,
                     fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSec)
            }
        }
    )
}
