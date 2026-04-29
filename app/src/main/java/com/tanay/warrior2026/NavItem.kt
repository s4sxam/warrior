
package com.tanay.warrior

// ─────────────────────────────────────────────────────────────────
// NavItem.kt  — v6.0.0 (Redesign)
//
// CHANGES:
//   • Reduced from 6 tabs to 5
//   • Renamed "War Room" → "Home" (clearer primary action)
//   • Renamed "Code" → "About"
//   • 5 is the max recommended tab count per Material guidelines
//   • Each tab has ONE clear purpose
//
// Tab purposes:
//   Home      → Daily action (log win/relapse, streak)
//   Arena     → Leaderboard motivation
//   Analysis  → Progress charts
//   Habits    → Manage tracked habits
//   About     → Settings, backup, info
// ─────────────────────────────────────────────────────────────────

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.tanay.warrior.data.ViewState

data class NavItem(val view: ViewState, val label: String, val icon: ImageVector)

val navItems = listOf(
    NavItem(ViewState.DASHBOARD,   "Home",     Icons.Filled.Home),
    NavItem(ViewState.LEADERBOARD, "Arena",    Icons.Filled.EmojiEvents),
    NavItem(ViewState.ANALYSIS,    "Progress", Icons.Filled.BarChart),
    NavItem(ViewState.HABITS,      "Habits",   Icons.Filled.FitnessCenter),
    NavItem(ViewState.ABOUT,       "About",    Icons.Filled.Shield),
)
