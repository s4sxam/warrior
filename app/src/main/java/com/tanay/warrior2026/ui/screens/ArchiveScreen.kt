package com.tanay.warrior2026.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.text.SimpleDateFormat
import java.util.*

private val MONTHS = listOf(
    "JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE",
    "JULY","AUGUST","SEPTEMBER","OCTOBER","NOVEMBER","DECEMBER"
)

@Composable
fun ArchiveScreen(state: WarriorState) {
    val sdf = remember { SimpleDateFormat("EEE MMM dd yyyy", Locale.US) }
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    var totalVictories = 0
    var totalDefeats = 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            "WARRIOR ARCHIVES",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = WarriorRed,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(20.dp))

        for (m in 0..currentMonth) {
            var mV = 0; var mD = 0
            val daysInMonth = Calendar.getInstance().apply { set(currentYear, m + 1, 0) }.get(Calendar.DAY_OF_MONTH)

            val dayDataList = (1..daysInMonth).map { day ->
                val cal = Calendar.getInstance().apply { set(currentYear, m, day) }
                val key = sdf.format(cal.time)
                state.history[key]
            }

            val hasData = dayDataList.any { it != null }
            if (!hasData) continue

            dayDataList.forEach { d ->
                if (d != null) {
                    if (d.status == "clean") mV++ else mD++
                }
            }
            totalVictories += mV; totalDefeats += mD

            // Month Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 22.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF080808))
                    .padding(20.dp)
            ) {
                Text(
                    "${MONTHS[m]} $currentYear",
                    fontSize = 11.sp,
                    color = WarriorRed,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(12.dp))

                val rows = dayDataList.chunked(7)
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        row.forEachIndexed { idx, d ->
                            val dayNum = rows.indexOf(row) * 7 + idx + 1
                            val bg = when (d?.status) {
                                "clean"  -> VictoryGreen
                                "failed" -> WarriorRed
                                else     -> CardBlack
                            }
                            val tc = when (d?.status) {
                                "clean"  -> Color.Black
                                "failed" -> Color.White
                                else     -> TextDimmer
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$dayNum", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = tc)
                                    if (d?.site != null) {
                                        Text("●", fontSize = 4.sp, color = Color.White.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                        repeat(7 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(5.dp))
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "V: $mV  |  D: $mD  |  ${if (mV + mD > 0) ((mV.toFloat() / (mV + mD)) * 100).toInt() else 0}% CLEAN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDim
                )
            }
        }

        // Grand Total
        val consistency = if (totalVictories + totalDefeats > 0)
            ((totalVictories.toFloat() / (totalVictories + totalDefeats)) * 100).toInt() else 0

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(25.dp))
                .background(WarriorRed)
                .padding(25.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("GRAND TOTAL SUM", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text("$totalVictories JAY  |  $totalDefeats PARAJAY", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("CONSISTENCY: $consistency%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.8f))
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
