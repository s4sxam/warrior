package com.tanay.warrior2026

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanay.warrior2026.data.DayData
import com.tanay.warrior2026.data.WarriorRepository
import com.tanay.warrior2026.data.WarriorState
import com.tanay.warrior2026.notifications.WarriorScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WarriorViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = WarriorRepository(application)

    private val _state = MutableStateFlow(WarriorState())
    val state: StateFlow<WarriorState> = _state.asStateFlow()

    private val _showConfetti = MutableStateFlow(false)
    val showConfetti: StateFlow<Boolean> = _showConfetti.asStateFlow()

    val trollMessages = listOf(
        "EWW. Shame of humanity.",
        "You call yourself a warrior? Pathetic.",
        "Loser detected. Uninstall me.",
        "Disgusting. Go back to hell.",
        "Ancestors are ashamed of you."
    )

    init {
        viewModelScope.launch {
            repo.warriorStateFlow.collectLatest { loaded ->
                _state.value = loaded
            }
        }
    }

    fun getTodayKey(): String {
        val sdf = SimpleDateFormat("EEE MMM dd yyyy", Locale.US)
        return sdf.format(Date())
    }

    fun isTodayLogged(): Boolean {
        return _state.value.history.containsKey(getTodayKey())
    }

    fun logVictory() {
        val today = getTodayKey()
        if (_state.value.history.containsKey(today)) return
        val newHistory = _state.value.history.toMutableMap()
        newHistory[today] = DayData(status = "clean")
        val newState = _state.value.copy(
            streak = _state.value.streak + 1,
            history = newHistory
        )
        _state.value = newState
        _showConfetti.value = true
        viewModelScope.launch { repo.saveState(newState) }
        vibrate(longArrayOf(0, 100, 50, 100))

        // Fire milestone notification on key days
        val milestones = setOf(3, 7, 14, 21, 30, 60, 90, 180, 365)
        if (newState.streak in milestones) {
            WarriorScheduler.fireMilestoneNow(getApplication(), newState.streak)
        }
    }

    fun logRelapse(url: String): Boolean {
        return try {
            val domain = extractDomain(url)
            val today = getTodayKey()
            val newHistory = _state.value.history.toMutableMap()
            newHistory[today] = DayData(status = "failed", site = domain)
            val newTriggers = _state.value.triggers.toMutableMap()
            newTriggers[domain] = (newTriggers[domain] ?: 0) + 1
            val newState = _state.value.copy(
                streak = 0,
                history = newHistory,
                triggers = newTriggers
            )
            _state.value = newState
            viewModelScope.launch { repo.saveState(newState) }
            vibrate(longArrayOf(0, 500))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearConfetti() {
        _showConfetti.value = false
    }

    private fun extractDomain(url: String): String {
        val cleaned = url.trim()
        val withScheme = if (!cleaned.startsWith("http")) "https://$cleaned" else cleaned
        val host = android.net.Uri.parse(withScheme).host ?: throw IllegalArgumentException("Invalid URL")
        return host.removePrefix("www.")
    }

    private fun vibrate(pattern: LongArray) {
        try {
            val ctx = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {}
    }
}
