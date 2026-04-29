package com.tanay.warrior.ui.screens

// ─────────────────────────────────────────────────────────────────
// HabitsScreen.kt  — v6.0.0 (Redesign)
//
// CHANGES:
//   • Removed GlowingHabitRune — visual noise, not usability
//   • One clear action per card: tap to activate, icons for edit/delete
//   • Streak and win rate visible inline — no need to navigate away
//   • Active habit badge is bold but not over-animated
//   • Add dialog simplified — emoji picker retained (useful metadata)
//   • Consistent button heights and spacing
//   • Architecture note: screen is display-only, all logic in ViewModel
// ─────────────────────────────────────────────────────────────────

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.window.Dialog
import com.tanay.warrior.WarriorViewModel
import com.tanay.warrior.data.Habit
import com.tanay.warrior.data.WarriorState
import com.tanay.warrior.data.todayKey
import com.tanay.warrior.ui.theme.*

@Composable
fun HabitsScreen(
    state: WarriorState,
    vm:    WarriorViewModel,
) {
    var showAddDialog   by remember { mutableStateOf(false) }
    var editHabit       by remember { mutableStateOf<Habit?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ─────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text          = "HABITS",
                        color         = TextPrimary,
                        fontSize      = 22.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = 3.sp,
                    )
                    Text(
                        text     = "Tap to activate · swipe or use icons to manage",
                        color    = TextTertiary,
                        fontSize = 11.sp,
                    )
                }
                IconButton(
                    onClick  = { showAddDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(WarriorRed.copy(alpha = 0.12f))
                        .border(1.dp, WarriorRed.copy(alpha = 0.4f), CircleShape),
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Add,
                        contentDescription = "Add habit",
                        tint               = WarriorRed,
                        modifier           = Modifier.size(20.dp),
                    )
                }
            }

            // ── Habit list ─────────────────────────────────────────────
            LazyColumn(
                modifier        = Modifier.fillMaxSize(),
                contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.habits, key = { it.id }) { habit ->
                    HabitCard(
                        habit    = habit,
                        isActive = habit.id == state.activeHabitId,
                        onSelect = { vm.switchHabit(habit.id) },
                        onEdit   = { editHabit = habit },
                        onDelete = {
                            if (state.habits.size > 1) confirmDeleteId = habit.id
                        },
                    )
                }

                // Bottom padding for dock
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        // ── Dialogs ───────────────────────────────────────────────────
        if (showAddDialog) {
            HabitEditDialog(
                title     = "Add Habit",
                initial   = null,
                onConfirm = { name, emoji ->
                    vm.addHabit(name, emoji)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false },
            )
        }

        editHabit?.let { h ->
            HabitEditDialog(
                title     = "Edit Habit",
                initial   = h,
                onConfirm = { name, emoji ->
                    vm.renameHabit(h.id, name, emoji)
                    editHabit = null
                },
                onDismiss = { editHabit = null },
            )
        }

        confirmDeleteId?.let { id ->
            DeleteConfirmDialog(
                onConfirm = {
                    vm.deleteHabit(id)
                    confirmDeleteId = null
                },
                onDismiss = { confirmDeleteId = null },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HABIT CARD
// ─────────────────────────────────────────────────────────────
@Composable
private fun HabitCard(
    habit:    Habit,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit:   () -> Unit,
    onDelete: () -> Unit,
) {
    val today   = todayKey()
    val winRate = if (habit.totalClean + habit.totalFailed > 0)
        (habit.totalClean * 100 / (habit.totalClean + habit.totalFailed)).toString() + "%"
    else "—"

    val borderColor = if (isActive) WarriorRed.copy(alpha = 0.6f) else BorderColor
    val bgColor     = if (isActive) WarriorRed.copy(alpha = 0.05f) else SurfaceDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Emoji + active indicator
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ElevatedCard),
            contentAlignment = Alignment.Center,
        ) {
            Text(habit.emoji, fontSize = 22.sp)
        }

        Spacer(Modifier.width(14.dp))

        // Name + stats
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = habit.name,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isActive) TextPrimary else TextSecondary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f, fill = false),
                )
                if (isActive) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text          = "ACTIVE",
                        fontSize      = 8.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = WarriorRed,
                        letterSpacing = 1.sp,
                        modifier      = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(WarriorRed.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat("🔥 ${habit.streak}d", "streak")
                MiniStat("✓ ${habit.totalClean}", "wins")
                MiniStat("$winRate", "rate")
            }
        }

        Spacer(Modifier.width(8.dp))

        // Edit / delete icons — secondary, quiet
        Row {
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit",
                    tint               = TextTertiary,
                    modifier           = Modifier.size(16.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint               = WarriorRed.copy(alpha = 0.5f),
                    modifier           = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Text(
        text       = "$value $label",
        fontSize   = 10.sp,
        color      = TextTertiary,
        fontWeight = FontWeight.Medium,
    )
}

// ─────────────────────────────────────────────────────────────
// ADD / EDIT DIALOG
// ─────────────────────────────────────────────────────────────
private val EMOJI_OPTIONS = listOf("🔥","💪","🧠","📚","🏃","💤","🥗","🚭","🎯","⚔️","🛡️","🌱")

@Composable
private fun HabitEditDialog(
    title:     String,
    initial:   Habit?,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name  by remember { mutableStateOf(initial?.name  ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "🔥") }
    val valid = name.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = TextPrimary)

            // Emoji picker row
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EMOJI_OPTIONS.forEach { e ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (emoji == e) WarriorRed.copy(alpha = 0.15f) else ElevatedCard)
                            .border(
                                1.dp,
                                if (emoji == e) WarriorRed.copy(alpha = 0.6f) else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(e, fontSize = 18.sp)
                    }
                }
            }

            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text("Habit name", color = TextTertiary, fontSize = 13.sp) },
                placeholder   = { Text("e.g. No Porn, No Smoking", color = TextTertiary, fontSize = 13.sp) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedTextColor      = TextPrimary,
                    unfocusedTextColor    = TextPrimary,
                    focusedBorderColor    = WarriorRed,
                    unfocusedBorderColor  = BorderColor,
                    focusedLabelColor     = WarriorRed,
                    cursorColor           = WarriorRed,
                ),
            )

            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.dp, BorderColor),
                ) {
                    Text("Cancel", color = TextSecondary)
                }
                Button(
                    onClick  = { if (valid) onConfirm(name.trim(), emoji) },
                    enabled  = valid,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = WarriorRed,
                        contentColor   = Color.White,
                    ),
                ) {
                    Text("Save", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// DELETE CONFIRM DIALOG
// ─────────────────────────────────────────────────────────────
@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.5.dp, WarriorRed.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Delete Habit?", fontSize = 17.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            Text(
                "All history for this habit will be permanently lost.",
                fontSize  = 13.sp,
                color     = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.dp, BorderColor),
                ) {
                    Text("Keep", color = TextSecondary)
                }
                Button(
                    onClick  = onConfirm,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = WarriorRed),
                ) {
                    Text("Delete", fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
    }
}
