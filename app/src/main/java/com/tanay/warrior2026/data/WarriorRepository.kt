package com.tanay.warrior2026.data

// [UPDATE] v2.0.0: Added profile, hasCompletedProfile, botsJson persistence
// [FIX]    v2.3.0: Added update-preference keys:
//                    - DISMISSED_VERSION: the latest version the user said "Later" to
//                    - PENDING_DOWNLOAD_ID: downloadId surviving process death
//                  Both allow the update system to remember state across restarts.
// [NEW]    v3.2.0: Added FIRST_RUN_DATE key — stored once on first profile completion.
//                  Used to stamp every bot's simulationStartDate so the 365-day
//                  heatmap only shows real data from app install day onwards.
// [NEW]    v4.0.0: Multi-habit support — HABITS_JSON replaces HISTORY + TRIGGERS.
//                  ACTIVE_HABIT_ID tracks the currently-selected habit.
//                  Legacy HISTORY / TRIGGERS keys are read once for migration.

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "warrior_prefs_v8")

class WarriorRepository(private val context: Context) {

    private object Keys {
        val HISTORY             = stringPreferencesKey("w_history_v8")       // legacy — migration only
        val TRIGGERS            = stringPreferencesKey("w_triggers_v8")      // legacy — migration only
        val ONBOARDING          = booleanPreferencesKey("w_onboarding_v8")
        // v2.0.0
        val USER_PROFILE        = stringPreferencesKey("w_profile_v8")
        val PROFILE_COMPLETE    = booleanPreferencesKey("w_profile_done_v8")
        val BOTS_JSON           = stringPreferencesKey("w_bots_v8")
        // v2.3.0 update persistence
        val DISMISSED_VERSION   = stringPreferencesKey("w_dismissed_version_v8")
        val PENDING_DOWNLOAD_ID = longPreferencesKey("w_pending_download_id_v8")
        // v3.2.0 first-run date
        val FIRST_RUN_DATE      = stringPreferencesKey("w_first_run_date_v8")
        // v4.0.0 multi-habit
        val HABITS_JSON         = stringPreferencesKey("w_habits_v9")
        val ACTIVE_HABIT_ID     = stringPreferencesKey("w_active_habit_v9")
    }

    val warriorStateFlow: Flow<WarriorState> = context.dataStore.data.map { prefs ->
        val onboarded       = prefs[Keys.ONBOARDING] ?: false
        val profileDone     = prefs[Keys.PROFILE_COMPLETE] ?: false
        val botsJson        = prefs[Keys.BOTS_JSON] ?: ""
        val activeHabitId   = prefs[Keys.ACTIVE_HABIT_ID] ?: ""

        val userProfile: UserProfile = runCatching {
            Json.decodeFromString<UserProfile>(prefs[Keys.USER_PROFILE] ?: "{}")
        }.getOrDefault(UserProfile())

        // v4.0.0: read new habits list, or migrate from legacy single-habit keys
        val habits: List<Habit> = runCatching {
            val json = prefs[Keys.HABITS_JSON]
            if (!json.isNullOrBlank()) {
                Json.decodeFromString<List<Habit>>(json)
            } else {
                // Migration path: wrap old history/triggers into a default habit
                val oldHistory: Map<String, DayData> = runCatching {
                    Json.decodeFromString<Map<String, DayData>>(prefs[Keys.HISTORY] ?: "{}")
                }.getOrDefault(emptyMap())
                val oldTriggers: Map<String, Int> = runCatching {
                    Json.decodeFromString<Map<String, Int>>(prefs[Keys.TRIGGERS] ?: "{}")
                }.getOrDefault(emptyMap())
                listOf(Habit(id = "habit_primary", name = "Main Habit", emoji = "🔥",
                    history = oldHistory, triggers = oldTriggers))
            }
        }.getOrDefault(listOf(Habit(id = "habit_primary", name = "Main Habit", emoji = "🔥")))

        val resolvedActiveId = if (habits.any { it.id == activeHabitId }) activeHabitId
                               else habits.firstOrNull()?.id ?: ""

        WarriorState(habits, resolvedActiveId, onboarded, userProfile, profileDone, botsJson)
    }

    suspend fun saveState(state: WarriorState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HABITS_JSON]      = Json.encodeToString(state.habits)
            prefs[Keys.ACTIVE_HABIT_ID]  = state.activeHabitId
            prefs[Keys.ONBOARDING]       = state.hasCompletedOnboarding
            prefs[Keys.USER_PROFILE]     = Json.encodeToString(state.userProfile)
            prefs[Keys.PROFILE_COMPLETE] = state.hasCompletedProfile
            prefs[Keys.BOTS_JSON]        = state.botsJson
        }
    }

    // ── v2.3.0: Update persistence helpers ───────────────────────────────────

    /** Returns the version string the user last dismissed, or "" if never dismissed. */
    suspend fun getDismissedVersion(): String =
        context.dataStore.data.first()[Keys.DISMISSED_VERSION] ?: ""

    /** Persists the version the user tapped "Later" on — survives app restarts. */
    suspend fun saveDismissedVersion(version: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISMISSED_VERSION] = version
        }
    }

    /** Persists the active DownloadManager job ID so it can be cleaned up on relaunch. */
    suspend fun savePendingDownloadId(id: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PENDING_DOWNLOAD_ID] = id
        }
    }

    /** Returns the saved download ID, or -1L if none. */
    suspend fun getPendingDownloadId(): Long =
        context.dataStore.data.first()[Keys.PENDING_DOWNLOAD_ID] ?: -1L

    /** Clears the saved download ID (call after download completes or is cancelled). */
    suspend fun clearPendingDownloadId() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.PENDING_DOWNLOAD_ID)
        }
    }

    // ── v3.2.0: First-run date ────────────────────────────────────────────────

    /**
     * Returns the persisted first-run date string (ISO_LOCAL_DATE), or "" if not set.
     * This is the date the user completed their profile for the first time.
     */
    suspend fun getFirstRunDate(): String =
        context.dataStore.data.first()[Keys.FIRST_RUN_DATE] ?: ""

    /**
     * Saves the first-run date. Only called once — on first profile completion.
     * Subsequent calls are no-ops (checked in ViewModel).
     */
    suspend fun saveFirstRunDate(date: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FIRST_RUN_DATE] = date
        }
    }

    // ── Backup / Restore (unchanged) ──────────────────────────────────────────

    fun exportJson(state: WarriorState): String = Json.encodeToString(state.history)

    fun importJson(existing: WarriorState, json: String): WarriorState? = runCatching {
        val imported: Map<String, DayData> = Json.decodeFromString(json)
        val activeHabit = existing.activeHabit ?: return null
        val updatedHabit = activeHabit.copy(history = activeHabit.history + imported)
        existing.copy(habits = existing.habits.map { if (it.id == updatedHabit.id) updatedHabit else it })
    }.getOrNull()
}