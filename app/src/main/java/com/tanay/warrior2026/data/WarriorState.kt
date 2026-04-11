package com.tanay.warrior2026.data

data class DayData(
    val status: String, // "clean" or "failed"
    val site: String? = null
)

data class WarriorState(
    val streak: Int = 0,
    val history: Map<String, DayData> = emptyMap(),
    val triggers: Map<String, Int> = emptyMap()
)

enum class ViewState {
    DASHBOARD, ANALYSIS, ARCHIVE, ABOUT
}
