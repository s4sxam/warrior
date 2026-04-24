package com.tanay.warrior2026

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.tanay.warrior2026.data.ViewState

data class NavItem(val view: ViewState, val label: String, val icon: ImageVector)

val navItems = listOf(
    NavItem(ViewState.DASHBOARD,   "War Room",  Icons.Filled.Home),
    NavItem(ViewState.LEADERBOARD, "Arena",     Icons.Filled.EmojiEvents),
    NavItem(ViewState.ANALYSIS,    "Analysis",  Icons.Filled.BarChart),
    NavItem(ViewState.ARCHIVE,     "Archives",  Icons.Filled.CalendarMonth),
    NavItem(ViewState.HABITS,      "Habits",    Icons.Filled.List),
    NavItem(ViewState.ABOUT,       "Code",      Icons.Filled.Shield),
)