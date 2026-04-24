package com.tanay.warrior2026.widget

// [NEW] v4.0.0: Home screen widget — shows active habit name, current streak,
//               and a tap-to-open action. Built with Jetpack Glance.

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tanay.warrior2026.MainActivity
import com.tanay.warrior2026.data.DayData
import com.tanay.warrior2026.data.Habit
import com.tanay.warrior2026.data.WarriorRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Widget definition ─────────────────────────────────────────────────────────

class StreakWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read active habit data directly from DataStore (no ViewModel in widget)
        val repo  = WarriorRepository(context)
        val state = repo.warriorStateFlow.first()
        val habit = state.activeHabit

        provideContent {
            WidgetContent(
                habitName  = habit?.name  ?: "Warrior",
                habitEmoji = habit?.emoji ?: "🔥",
                streak     = habit?.streak ?: 0,
                bestStreak = habit?.bestStreak ?: 0,
                todayLogged = habit?.isTodayLogged() ?: false
            )
        }
    }
}

// ── Widget UI ─────────────────────────────────────────────────────────────────

@Composable
private fun WidgetContent(
    habitName:   String,
    habitEmoji:  String,
    streak:      Int,
    bestStreak:  Int,
    todayLogged: Boolean
) {
    val Red      = ColorProvider(Color(0xFFCC0000))
    val White    = ColorProvider(Color(0xFFEEEEEE))
    val Muted    = ColorProvider(Color(0xFF888888))
    val Green    = ColorProvider(Color(0xFF2ECC71))
    val DarkBg   = ColorProvider(Color(0xFF0D0D0D))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Habit name row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = habitEmoji,
                    style = TextStyle(fontSize = 18.sp)
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    text  = habitName.uppercase(),
                    style = TextStyle(
                        color      = Muted,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            Spacer(GlanceModifier.height(6.dp))

            // Big streak number
            Text(
                text  = "$streak",
                style = TextStyle(
                    color      = Red,
                    fontSize   = 48.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            // "days" label
            Text(
                text  = if (streak == 1) "DAY CLEAN" else "DAYS CLEAN",
                style = TextStyle(
                    color      = White,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(GlanceModifier.height(8.dp))

            // Today status + best streak row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = if (todayLogged) "✅ Logged" else "⏳ Not logged yet",
                    style = TextStyle(
                        color    = if (todayLogged) Green else Muted,
                        fontSize = 11.sp
                    )
                )
                Spacer(GlanceModifier.width(12.dp))
                Text(
                    text  = "Best: ${bestStreak}d",
                    style = TextStyle(color = Muted, fontSize = 11.sp)
                )
            }
        }
    }
}

// ── Receiver ─────────────────────────────────────────────────────────────────

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}
