package com.tanay.warrior.ui.screens

// [NEW] v4.3.0: Achievements screen.
//
// Layout/motion pattern follows Google Play Games' Achievements page:
// a grid of icon-cards grouped by category header showing "(earned/total)",
// each card showing a lock state OR a progress ring/percentage, tap opens a
// detail reveal with the full story. This is a from-scratch build in the
// app's own Warrior Red/Black visual language — no Play Games icon assets,
// colors, or copy were copied; only the general layout/motion shape.
//
// Unlock state is never stored separately — every card's progress comes
// straight from Achievement.progressOf(state), which reads real habit data
// (see data/Achievements.kt). The only persisted bit here is which unlocks
// have already played their reveal animation once (WarriorViewModel.
// seenAchievementIds / markAchievementSeen), so re-opening this screen
// doesn't replay confetti for something unlocked days ago.

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.border
import com.tanay.warrior.data.Achievement
import com.tanay.warrior.data.AchievementCategory
import com.tanay.warrior.data.AchievementProgress
import com.tanay.warrior.data.AchievementRarity
import com.tanay.warrior.data.WarriorState
import com.tanay.warrior.ui.theme.*

// ─────────────────────────────────────────────────────────────
// Rarity → color. Follows the same rising-value visual grammar Play
// Games uses (green → blue → purple → gold as rarity increases), but
// built entirely from colors already in this app's palette — no new
// hardcoded brand colors introduced.
// ─────────────────────────────────────────────────────────────
private fun rarityColor(rarity: AchievementRarity): Color = when (rarity) {
    AchievementRarity.COMMON    -> TextSecondary
    AchievementRarity.UNCOMMON  -> VictoryGreen
    AchievementRarity.RARE      -> Gold                    // "Arena Blue" in this app's palette
    AchievementRarity.EPIC      -> Color(0xFF9C27B0)        // Violet — already used in ACCENT_PRESETS
    AchievementRarity.LEGENDARY -> Color(0xFFFFD700)        // Gold-yellow — already used for rank/best-streak accents
}

private fun rarityLabel(rarity: AchievementRarity): String = when (rarity) {
    AchievementRarity.COMMON    -> "COMMON"
    AchievementRarity.UNCOMMON  -> "UNCOMMON"
    AchievementRarity.RARE      -> "RARE"
    AchievementRarity.EPIC      -> "EPIC"
    AchievementRarity.LEGENDARY -> "LEGENDARY"
}

// ─────────────────────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────────────────────

/**
 * [onBack] closes this screen (wired to a back-arrow header + system back).
 * [regionalRank] / [globalRank] are the user's live 1-indexed leaderboard
 * position (null if not yet on a board) — needed for the two ARENA
 * achievements, which can't be derived from WarriorState alone. See
 * data/Achievements.kt's achievementsWithArenaRank().
 * [seenIds] / [onMarkSeen] track which unlock reveals have already played.
 */
@Composable
fun AchievementsScreen(
    state: WarriorState,
    regionalRank: Int?,
    globalRank: Int?,
    seenIds: Set<String>,
    onMarkSeen: (String) -> Unit,
    onBack: () -> Unit
) {
    val achievements = remember(regionalRank, globalRank) {
        com.tanay.warrior.data.achievementsWithArenaRank(
            com.tanay.warrior.data.ALL_ACHIEVEMENTS, regionalRank, globalRank
        )
    }

    // Recompute progress whenever the underlying habit data changes.
    val withProgress = remember(achievements, state) {
        achievements.map { a -> a to a.progressOf(state) }
    }

    val grouped = remember(withProgress) {
        withProgress.groupBy { it.first.category }
    }

    val totalUnlocked = remember(withProgress) { withProgress.count { it.second.isComplete } }
    val totalCount = withProgress.size

    var selected by remember { mutableStateOf<Pair<Achievement, AchievementProgress>?>(null) }

    // v4.3.0 — detect achievements that just became complete but haven't
    // shown their reveal yet, so we can play the one-time unlock animation
    // the moment this screen renders them (mirrors Play Games surfacing a
    // toast/reveal the first time you see a freshly-unlocked achievement).
    val newlyUnlocked = remember(withProgress, seenIds) {
        withProgress.filter { (a, p) -> p.isComplete && a.id !in seenIds }
    }
    var revealQueue by remember(newlyUnlocked.map { it.first.id }) {
        mutableStateOf(newlyUnlocked)
    }

    // v4.3.0 — auto-open the sheet for a pending unlock reveal. Keyed on both
    // the queue's head AND `selected == null` so that dismissing a manually-
    // opened card (selected → null) while a reveal is still queued correctly
    // re-triggers this effect and shows the queued reveal next, instead of
    // silently losing it because the queue's head id itself didn't change.
    LaunchedEffect(revealQueue.firstOrNull()?.first?.id, selected == null) {
        if (selected != null) return@LaunchedEffect
        val next = revealQueue.firstOrNull() ?: return@LaunchedEffect
        selected = next
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalScreenBg.current)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", fontSize = 22.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(4.dp))
                Text("ACHIEVEMENTS", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                item {
                    // Overall summary card — mirrors Play Games' per-game
                    // "(80/94)" header, but as one hero card up top since this
                    // screen isn't scoped to one external "game".
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("YOUR PROGRESS", fontSize = 10.sp, color = TextTertiary,
                                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("$totalUnlocked / $totalCount unlocked", fontSize = 16.sp,
                                    fontWeight = FontWeight.Black, color = TextPrimary)
                            }
                            OverallRing(fraction = if (totalCount > 0) totalUnlocked.toFloat() / totalCount else 0f)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                AchievementCategory.entries.forEach { category ->
                    val entries = grouped[category] ?: return@forEach
                    val unlockedInCat = entries.count { it.second.isComplete }

                    item(key = "header_${category.name}") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.displayName, fontSize = 13.sp, fontWeight = FontWeight.Black,
                                color = TextPrimary, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                            Text("$unlockedInCat/${entries.size}", fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold, color = TextTertiary)
                        }
                    }

                    item(key = "grid_${category.name}") {
                        // A LazyVerticalGrid nested inside a LazyColumn item needs a
                        // fixed height strategy since both are lazy-scrolling by
                        // default; using a non-lazy grid-via-rows here instead
                        // keeps this simple and avoids nested-scroll conflicts.
                        AchievementGridRows(
                            entries = entries,
                            onCardClick = { a, p -> selected = a to p }
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }

                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    // ── Detail / unlock reveal sheet ──
    val current = selected
    if (current != null) {
        val (achievement, progress) = current
        val isNewReveal = achievement.id in revealQueue.map { it.first.id }
        AchievementDetailSheet(
            achievement = achievement,
            progress    = progress,
            isNewReveal = isNewReveal,
            onDismiss   = {
                if (isNewReveal) {
                    onMarkSeen(achievement.id)
                    revealQueue = revealQueue.filter { it.first.id != achievement.id }
                }
                selected = null
            }
        )
    }
}

/**
 * Renders [entries] as a 3-column grid using plain Rows — chosen over
 * LazyVerticalGrid because this content already lives inside a LazyColumn
 * item (one per category), and nesting two lazy-scrolling containers
 * without a fixed height causes measurement issues. Category grids here
 * are always small (a handful of achievements), so a non-lazy layout costs
 * nothing in practice.
 */
@Composable
private fun AchievementGridRows(
    entries: List<Pair<Achievement, AchievementProgress>>,
    onCardClick: (Achievement, AchievementProgress) -> Unit
) {
    val columns = 3
    val rows = entries.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { (a, p) ->
                    AchievementCard(
                        achievement = a,
                        progress    = p,
                        modifier    = Modifier.weight(1f),
                        onClick     = { onCardClick(a, p) }
                    )
                }
                // Pad the last, incomplete row so cards stay a consistent
                // width instead of stretching to fill the row.
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    progress: AchievementProgress,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val unlocked = progress.isComplete
    val rColor = rarityColor(achievement.rarity)

    val animFraction by animateFloatAsState(
        targetValue   = progress.fraction,
        animationSpec = tween(800, easing = EaseOutCubic),
        label         = "card_progress_${achievement.id}"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBlack)
            .border(1.dp, if (unlocked) rColor.copy(alpha = 0.4f) else BorderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
            // Progress ring — always drawn (full ring at 100% doubles as the
            // "unlocked" halo), matching Play Games' ring-around-icon pattern
            // for in-progress achievements.
            Canvas(modifier = Modifier.size(56.dp)) {
                val strokeW = 4.dp.toPx()
                val inset = strokeW / 2f
                drawArc(
                    color = BorderColor,
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - strokeW, size.height - strokeW),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                drawArc(
                    color = if (unlocked) rColor else rColor.copy(alpha = 0.7f),
                    startAngle = -90f, sweepAngle = 360f * animFraction, useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - strokeW, size.height - strokeW),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (unlocked) rColor.copy(alpha = 0.15f) else Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    achievement.icon,
                    fontSize = 20.sp,
                    modifier = Modifier.alpha(if (unlocked) 1f else 0.35f)
                )
            }
            if (!unlocked) {
                // Lock overlay — small badge bottom-right of the icon circle,
                // matching the lock-corner treatment in the reference screenshots.
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0A0A))
                        .border(1.dp, BorderColor, CircleShape)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔒", fontSize = 9.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            achievement.title,
            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
            color = if (unlocked) TextPrimary else TextTertiary,
            textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
        Spacer(Modifier.height(4.dp))
        if (unlocked) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(rColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(rarityLabel(achievement.rarity), fontSize = 7.sp, fontWeight = FontWeight.Black,
                    color = rColor, letterSpacing = 0.5.sp)
            }
        } else {
            Text(
                "${progress.current}/${progress.target}",
                fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextDim
            )
        }
    }
}

/** Small ring for the overall-progress hero card at the top of the screen. */
@Composable
private fun OverallRing(fraction: Float) {
    val anim by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "overall_ring"
    )
    Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(52.dp)) {
            val strokeW = 6.dp.toPx()
            val inset = strokeW / 2f
            drawArc(
                color = CardBlack,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - strokeW, size.height - strokeW),
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            drawArc(
                color = WarriorRed,
                startAngle = -90f, sweepAngle = 360f * anim, useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - strokeW, size.height - strokeW),
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }
        Text("${(fraction * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextPrimary)
    }
}

/**
 * Detail sheet — doubles as both the "tap any card to read its story"
 * view AND the one-time unlock reveal (when [isNewReveal] is true, the
 * icon does an entrance scale/fade the way Play Games' reveal sheet
 * animates in on a fresh unlock).
 */
@Composable
private fun AchievementDetailSheet(
    achievement: Achievement,
    progress: AchievementProgress,
    isNewReveal: Boolean,
    onDismiss: () -> Unit
) {
    val rColor = rarityColor(achievement.rarity)
    val unlocked = progress.isComplete

    // Entrance animation for the icon — a quick overshoot scale-in, most
    // noticeable (and most appropriate) on a genuine new unlock, but kept
    // subtle and consistent even for a re-opened already-seen achievement
    // so the sheet doesn't feel static.
    var animateIn by remember(achievement.id) { mutableStateOf(false) }
    LaunchedEffect(achievement.id) { animateIn = true }
    val iconScale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "reveal_scale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(300),
        label = "reveal_alpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0A0A0A))
                .border(1.dp, rColor.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isNewReveal) {
                Text("ACHIEVEMENT UNLOCKED", fontSize = 11.sp, fontWeight = FontWeight.Black,
                    color = rColor, letterSpacing = 2.sp)
                Spacer(Modifier.height(14.dp))
            }

            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(rColor.copy(alpha = if (unlocked) 0.18f else 0.08f))
                    .border(2.dp, rColor.copy(alpha = if (unlocked) 0.6f else 0.25f), CircleShape)
                    .scale(iconScale)
                    .alpha(iconAlpha),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    achievement.icon,
                    fontSize = 36.sp,
                    modifier = Modifier.alpha(if (unlocked) 1f else 0.4f)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(achievement.title, fontSize = 20.sp, fontWeight = FontWeight.Black,
                color = TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(rColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(rarityLabel(achievement.rarity), fontSize = 9.sp, fontWeight = FontWeight.Black,
                    color = rColor, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                achievement.story,
                fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 19.sp
            )

            if (!unlocked) {
                Spacer(Modifier.height(18.dp))
                val animPct by animateFloatAsState(
                    targetValue = progress.fraction,
                    animationSpec = tween(900, easing = EaseOutCubic),
                    label = "sheet_progress"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardBlack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animPct)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(rColor)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text("${progress.current} / ${progress.target}", fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, color = TextTertiary)
            }

            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(rColor.copy(alpha = 0.12f))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isNewReveal) "NICE" else "CLOSE", fontSize = 13.sp,
                    fontWeight = FontWeight.Black, color = rColor, letterSpacing = 1.sp)
            }
        }
    }
}
