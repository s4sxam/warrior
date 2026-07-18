package com.tanay.warrior.widget

// [NEW] v4.0.0: Home screen widget — shows active habit name, current streak,
//               and a tap-to-open action. Built with Jetpack Glance.
// [NEW] v4.1.0: Yes/No quick-log — tap ✅ to mark today clean or ❌ to log a
//               fail, right from the widget, no need to open the app. Once
//               today is logged the buttons are replaced with a one-line
//               status so the widget stays small. Stat row now shows total
//               clean days and this month's clean % alongside the streak.

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tanay.warrior.MainActivity
import com.tanay.warrior.data.DayData
import com.tanay.warrior.data.Habit
import com.tanay.warrior.data.WarriorRepository
import com.tanay.warrior.data.todayKey
import com.tanay.warrior.notifications.WarriorScheduler
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ── Widget definition ─────────────────────────────────────────────────────────

class StreakWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read active habit data directly from DataStore (no ViewModel in widget)
        val repo    = WarriorRepository(context)
        val state   = repo.warriorStateFlow.first()
        val habit   = state.activeHabit
        val today   = habit?.history?.get(todayKey())

        provideContent {
            WidgetContent(
                habitName    = habit?.name  ?: "Warrior",
                habitEmoji   = habit?.emoji ?: "🔥",
                streak       = habit?.streak ?: 0,
                totalClean   = habit?.totalClean ?: 0,
                monthPercent = habit?.monthCleanPercent ?: 0,
                todayStatus  = today?.status,
                failCount    = today?.relapseCount ?: 0
            )
        }
    }
}

// ── Quick-log actions ───────────────────────────────────────────────────────
// Mirror WarriorViewModel.logVictory() / logRelapse(), but run directly
// against the repository since the widget has no ViewModel of its own.

class LogCleanAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repo   = WarriorRepository(context)
        val state  = repo.warriorStateFlow.first()
        val active = state.activeHabit ?: return
        val today  = todayKey()

        // Same guard as the in-app flow: can't log clean if today already has
        // an entry (clean or failed). Use the app's Undo to clear it first.
        if (active.history.containsKey(today)) return

        val updatedHabit = active.copy(history = active.history + (today to DayData(status = "clean")))
        val newState = state.copy(habits = state.habits.map { if (it.id == updatedHabit.id) updatedHabit else it })
        repo.saveState(newState)
        StreakWidget().update(context, glanceId)

        val milestones = setOf(3, 7, 14, 21, 30, 60, 90, 180, 365)
        if (updatedHabit.streak in milestones) {
            WarriorScheduler.fireMilestoneNow(context, updatedHabit.streak)
        }
    }
}

class LogFailAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repo   = WarriorRepository(context)
        val state  = repo.warriorStateFlow.first()
        val active = state.activeHabit ?: return
        val today  = todayKey()

        val nowTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val existing = active.history[today]
        val updatedDay = DayData(
            status       = "failed",
            site         = existing?.site, // no domain capture from the widget — use the app for that detail
            relapseCount = (existing?.relapseCount ?: 0) + 1,
            lastFailTime = nowTime
        )
        val updatedHabit = active.copy(history = active.history + (today to updatedDay))
        val newState = state.copy(habits = state.habits.map { if (it.id == updatedHabit.id) updatedHabit else it })
        repo.saveState(newState)
        StreakWidget().update(context, glanceId)

        WarriorScheduler.rescheduleEveningToFailTime(context, nowTime)
    }
}

// ── Widget UI ─────────────────────────────────────────────────────────────────

@Composable
private fun WidgetContent(
    habitName:    String,
    habitEmoji:   String,
    streak:       Int,
    totalClean:   Int,
    monthPercent: Int,
    todayStatus:  String?,
    failCount:    Int
) {
    val Red    = ColorProvider(Color(0xFFCC0000))
    val White  = ColorProvider(Color(0xFFEEEEEE))
    val Muted  = ColorProvider(Color(0xFF888888))
    val Green  = ColorProvider(Color(0xFF2ECC71))
    val GreenBg = ColorProvider(Color(0xFF163D26))
    val RedBg   = ColorProvider(Color(0xFF3D1616))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Habit name row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = habitEmoji,
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    text  = habitName.uppercase(),
                    style = TextStyle(
                        color      = Muted,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            Spacer(GlanceModifier.height(2.dp))

            // Big streak number
            Text(
                text  = "$streak",
                style = TextStyle(
                    color      = Red,
                    fontSize   = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            // "days" label
            Text(
                text  = if (streak == 1) "DAY CLEAN" else "DAYS CLEAN",
                style = TextStyle(
                    color      = White,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(GlanceModifier.height(4.dp))

            // Compact stat row: total clean days + this month's clean %
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = "Clean: ${totalClean}d",
                    style = TextStyle(color = Muted, fontSize = 10.sp)
                )
                Spacer(GlanceModifier.width(10.dp))
                Text(
                    text  = "Month: ${monthPercent}%",
                    style = TextStyle(color = Muted, fontSize = 10.sp)
                )
            }

            Spacer(GlanceModifier.height(6.dp))

            if (todayStatus == null) {
                // Not logged yet today — show Yes / No quick-log buttons
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(GreenBg)
                            .clickable(actionRunCallback<LogCleanAction>())
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "✅ Yes",
                            style = TextStyle(color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        )
                    }
                    Spacer(GlanceModifier.width(6.dp))
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(RedBg)
                            .clickable(actionRunCallback<LogFailAction>())
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "❌ No",
                            style = TextStyle(color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        )
                    }
                }
            } else {
                // Already logged today — show status instead of buttons
                Text(
                    text  = if (todayStatus == "clean") "✅ Today: Clean"
                            else if (failCount > 1) "❌ Today: Failed (${failCount}x)"
                            else "❌ Today: Failed",
                    style = TextStyle(
                        color      = if (todayStatus == "clean") Green else Red,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// ── Receiver ─────────────────────────────────────────────────────────────────

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}
