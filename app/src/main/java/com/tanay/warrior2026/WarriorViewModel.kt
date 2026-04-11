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
import com.tanay.warrior2026.data.todayKey
import com.tanay.warrior2026.notifications.WarriorScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
            repo.warriorStateFlow.collectLatest { _state.value = it }
        }
    }

    fun completeOnboarding() {
        val new = _state.value.copy(hasCompletedOnboarding = true)
        _state.value = new
        viewModelScope.launch { repo.saveState(new) }
    }

    fun logVictory() {
        val today = todayKey()
        if (_state.value.history.containsKey(today)) return
        val new = _state.value.copy(
            history = _state.value.history + (today to DayData(status = "clean"))
        )
        _state.value = new
        _showConfetti.value = true
        viewModelScope.launch { repo.saveState(new) }
        vibrate(longArrayOf(0, 100, 50, 100))
        val milestones = setOf(3, 7, 14, 21, 30, 60, 90, 180, 365)
        if (new.streak in milestones) WarriorScheduler.fireMilestoneNow(getApplication(), new.streak)
    }

    fun logRelapse(url: String): Boolean = runCatching {
        val domain = if (url.isBlank() || url == "unknown") "unknown"
                     else extractDomain(url)
        val today  = todayKey()
        val newTriggers = _state.value.triggers.toMutableMap()
        if (domain != "unknown") newTriggers[domain] = (newTriggers[domain] ?: 0) + 1
        val new = _state.value.copy(
            history  = _state.value.history + (today to DayData(status = "failed", site = domain)),
            triggers = newTriggers
        )
        _state.value = new
        viewModelScope.launch { repo.saveState(new) }
        vibrate(longArrayOf(0, 500))
        true
    }.getOrDefault(false)

    fun undoToday() {
        val today = todayKey()
        if (!_state.value.history.containsKey(today)) return
        val new = _state.value.copy(history = _state.value.history - today)
        _state.value = new
        viewModelScope.launch { repo.saveState(new) }
    }

    fun clearConfetti() { _showConfetti.value = false }

    fun exportJson(): String = repo.exportJson(_state.value)

    fun importJson(json: String): Boolean {
        val merged = repo.importJson(_state.value, json) ?: return false
        _state.value = merged
        viewModelScope.launch { repo.saveState(merged) }
        return true
    }

    private fun extractDomain(url: String): String {
        val cleaned   = url.trim()
        val withScheme = if (!cleaned.startsWith("http")) "https://$cleaned" else cleaned
        val host = android.net.Uri.parse(withScheme).host ?: throw IllegalArgumentException("bad url")
        return host.removePrefix("www.")
    }

    private fun vibrate(pattern: LongArray) {
        try {
            val ctx = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ctx.getSystemService(VibratorManager::class.java)
                    ?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                else {
                    @Suppress("DEPRECATION") v?.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {}
    }
}