package com.tanay.warrior2026.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanay.warrior2026.data.WarriorState
import com.tanay.warrior2026.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private data class ArchiveMonth(
    val label: String,
    val year: Int,
    val monthValue: Int, // 1–12
    val victories: Int,
    val defeats: Int,
    val dayStatuses: List<String?> // null=no data, "clean", "failed"
)

private fun buildArchiveMonths(state: WarriorState): List<ArchiveMonth> {
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val now = LocalDate.now()
    return (0..11).reversed().mapNotNull { offset ->
        val target = now.minusMonths(offset.toLong())
        val daysInMonth = target.month.length(target.isLeapYear)
        var v = 0; var d = 0
        val statuses = (1..daysInMonth).map { day ->
            val key = target.withDayOfMonth(day).format(fmt)
            when (val s = state.history[key]?.status) {
                "clean"  -> { v++; s }
                "failed" -> { d++; s }
                else     -> null
            }
        }
        if (v + d == 0) null
        else ArchiveMonth(
            label      = target.month.getDisplayName(TextStyle.FULL, Locale.US).uppercase(),
            year       = target.year,
            monthValue = target.monthValue,
            victories  = v,
            defeats    = d,
            dayStatuses = statuses
        )
    }
}

@Composable
fun ArchiveScreen(state: WarriorState) {
    val months = remember(state.history) { buildArchiveMonths(state) }

    if (months.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(BgBlack), contentAlignment = Alignment.Center) {
            Text("NO DATA YET.\nSTART LOGGING.", fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold, color = TextTertiary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        return
    }

    val totalV = months.sumOf { it.victories }
    val totalD = months.sumOf { it.defeats }
    val totalC = if (totalV + totalD > 0) ((totalV.toFloat() / (totalV + totalD)) * 100).toInt() else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgBlack),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("WARRIOR ARCHIVES", fontSize = 22.sp, fontWeight = FontWeight.Black,
                color = WarriorRed, letterSpacing = 2.sp)
        }

        items(items = months, key = { "${it.year}-${it.monthValue}" }) { m ->
            ArchiveMonthCard(m)
        }

        // Grand total
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(WarriorRed)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ALL-TIME", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("$totalV CLEAN  ·  $totalD FAILED", fontSize = 18.sp,
                        fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("$totalC% CONSISTENCY", fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ArchiveMonthCard(m: ArchiveMonth) {
    val consistency = if (m.victories + m.defeats > 0)
        ((m.victories.toFloat() / (m.victories + m.defeats)) * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBlack)
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${m.label} ${m.year}", fontSize = 12.sp, color = WarriorRed,
                fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text("$consistency% CLEAN", fontSize = 10.sp,
                color = if (consistency >= 70) VictoryGreen else TextTertiary,
                fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(12.dp))

        // DOW headers
        val dowHeaders = listOf("S","M","T","W","T","F","S")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            dowHeaders.forEach { d ->
                Text(d, modifier = Modifier.weight(1f), fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold, color = TextTertiary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(4.dp))

        // Weekday offset for this month
        val firstDay = LocalDate.of(m.year, m.monthValue, 1)
        val startOffset = firstDay.dayOfWeek.value % 7
        val cells: List<Int?> = List(startOffset) { null } + m.dayStatuses.indices.map { it + 1 }
        val rows = cells.chunked(7)

        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val padded = row + List(7 - row.size) { null }
                padded.forEach { day ->
                    if (day == null) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val status = m.dayStatuses.getOrNull(day - 1)
                        val bg = when (status) {
                            "clean"  -> VictoryGreen
                            "failed" -> WarriorRed
                            else     -> Color(0xFF1A1A1A)
                        }
                        val tc = when (status) {
                            "clean"  -> Color.Black
                            "failed" -> Color.White
                            else     -> Color(0xFF444444)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$day", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = tc)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("✅ ${m.victories} CLEAN", fontSize = 10.sp, color = VictoryGreen,
                fontWeight = FontWeight.ExtraBold)
            Text("❌ ${m.defeats} FAILED", fontSize = 10.sp, color = WarriorRed,
                fontWeight = FontWeight.ExtraBold)
        }
    }
}