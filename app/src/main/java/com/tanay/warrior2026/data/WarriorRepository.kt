package com.tanay.warrior2026.data

// [UPDATE] v2.0.0: Added profile, hasCompletedProfile, botsJson persistence

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "warrior_prefs_v8")

class WarriorRepository(private val context: Context) {

    private object Keys {
        val HISTORY             = stringPreferencesKey("w_history_v8")
        val TRIGGERS            = stringPreferencesKey("w_triggers_v8")
        val ONBOARDING          = booleanPreferencesKey("w_onboarding_v8")
        // v2.0.0
        val USER_PROFILE        = stringPreferencesKey("w_profile_v8")
        val PROFILE_COMPLETE    = booleanPreferencesKey("w_profile_done_v8")
        val BOTS_JSON           = stringPreferencesKey("w_bots_v8")
    }

    val warriorStateFlow: Flow<WarriorState> = context.dataStore.data.map { prefs ->
        val history: Map<String, DayData> = runCatching {
            Json.decodeFromString<Map<String, DayData>>(prefs[Keys.HISTORY] ?: "{}")
        }.getOrDefault(emptyMap())

        val triggers: Map<String, Int> = runCatching {
            Json.decodeFromString<Map<String, Int>>(prefs[Keys.TRIGGERS] ?: "{}")
        }.getOrDefault(emptyMap())

        val onboarded       = prefs[Keys.ONBOARDING] ?: false
        val profileDone     = prefs[Keys.PROFILE_COMPLETE] ?: false
        val botsJson        = prefs[Keys.BOTS_JSON] ?: ""

        val userProfile: UserProfile = runCatching {
            Json.decodeFromString<UserProfile>(prefs[Keys.USER_PROFILE] ?: "{}")
        }.getOrDefault(UserProfile())

        WarriorState(history, triggers, onboarded, userProfile, profileDone, botsJson)
    }

    suspend fun saveState(state: WarriorState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HISTORY]          = Json.encodeToString(state.history)
            prefs[Keys.TRIGGERS]         = Json.encodeToString(state.triggers)
            prefs[Keys.ONBOARDING]       = state.hasCompletedOnboarding
            prefs[Keys.USER_PROFILE]     = Json.encodeToString(state.userProfile)
            prefs[Keys.PROFILE_COMPLETE] = state.hasCompletedProfile
            prefs[Keys.BOTS_JSON]        = state.botsJson
        }
    }

    fun exportJson(state: WarriorState): String = Json.encodeToString(state.history)

    fun importJson(existing: WarriorState, json: String): WarriorState? = runCatching {
        val imported: Map<String, DayData> = Json.decodeFromString(json)
        existing.copy(history = existing.history + imported)
    }.getOrNull()
}