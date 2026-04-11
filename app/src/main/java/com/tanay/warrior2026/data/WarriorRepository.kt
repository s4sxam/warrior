package com.tanay.warrior2026.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "warrior_prefs_v7")

class WarriorRepository(private val context: Context) {

    private object Keys {
        val HISTORY     = stringPreferencesKey("w_history_v7")
        val TRIGGERS    = stringPreferencesKey("w_triggers_v7")
        val ONBOARDING  = booleanPreferencesKey("w_onboarding_v7")
    }

    val warriorStateFlow: Flow<WarriorState> = context.dataStore.data.map { prefs ->
        val history: Map<String, DayData> = runCatching {
            Json.decodeFromString(prefs[Keys.HISTORY] ?: "{}")
        }.getOrDefault(emptyMap())
        val triggers: Map<String, Int> = runCatching {
            Json.decodeFromString(prefs[Keys.TRIGGERS] ?: "{}")
        }.getOrDefault(emptyMap())
        val onboarded = prefs[Keys.ONBOARDING] ?: false
        WarriorState(history, triggers, onboarded)
    }

    suspend fun saveState(state: WarriorState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HISTORY]    = Json.encodeToString(state.history)
            prefs[Keys.TRIGGERS]   = Json.encodeToString(state.triggers)
            prefs[Keys.ONBOARDING] = state.hasCompletedOnboarding
        }
    }

    /** Returns a JSON string for export/backup */
    fun exportJson(state: WarriorState): String = Json.encodeToString(state.history)

    /** Imports from a JSON backup string â€” merges with existing history */
    fun importJson(existing: WarriorState, json: String): WarriorState? = runCatching {
        val imported: Map<String, DayData> = Json.decodeFromString(json)
        existing.copy(history = existing.history + imported)
    }.getOrNull()
}