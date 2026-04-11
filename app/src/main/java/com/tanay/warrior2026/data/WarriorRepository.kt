package com.tanay.warrior2026.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "warrior_prefs_v6")

class WarriorRepository(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val STREAK = intPreferencesKey("w_streak_v6")
        val HISTORY = stringPreferencesKey("w_history_v6")
        val TRIGGERS = stringPreferencesKey("w_triggers_v6")
    }

    val warriorStateFlow: Flow<WarriorState> = context.dataStore.data.map { prefs ->
        val streak = prefs[Keys.STREAK] ?: 0
        val historyJson = prefs[Keys.HISTORY] ?: "{}"
        val triggersJson = prefs[Keys.TRIGGERS] ?: "{}"

        val historyType = object : TypeToken<Map<String, DayData>>() {}.type
        val triggersType = object : TypeToken<Map<String, Int>>() {}.type

        val history: Map<String, DayData> = try {
            gson.fromJson(historyJson, historyType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
        val triggers: Map<String, Int> = try {
            gson.fromJson(triggersJson, triggersType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        WarriorState(streak, history, triggers)
    }

    suspend fun saveState(state: WarriorState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STREAK] = state.streak
            prefs[Keys.HISTORY] = gson.toJson(state.history)
            prefs[Keys.TRIGGERS] = gson.toJson(state.triggers)
        }
    }
}
