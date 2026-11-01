package com.tanay.warrior.nnn.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tanay.warrior.nnn.domain.NnnState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Separate DataStore FILE from the ordinary Warrior prefs ("warrior_prefs_v8") so backup rules can exclude
// exactly this one. The file is files/datastore/nnn_private_v1.preferences_pb — keep res/xml/backup_rules.xml
// and res/xml/data_extraction_rules.xml in sync with NAME.
private val Context.nnnDataStore by preferencesDataStore(name = NnnDataStore.NAME)

class NnnDataStore(context: Context) {
    private val store = context.applicationContext.nnnDataStore

    val state: Flow<NnnState> = store.data.map { decode(it[KEY]) }

    /** Atomic read-modify-write. */
    suspend fun update(transform: (NnnState) -> NnnState) {
        store.edit { prefs -> prefs[KEY] = encode(transform(decode(prefs[KEY]))) }
    }

    /** JSON of the whole NNN state, for the user-initiated export flow. */
    fun encodeForExport(state: NnnState): String = encode(state)

    companion object {
        const val NAME = "nnn_private_v1"
        private val KEY = stringPreferencesKey("state_json_v1")
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

        private fun encode(state: NnnState): String = json.encodeToString(state.toDto())

        // Never logs the payload — only that decoding failed.
        private fun decode(raw: String?): NnnState {
            if (raw.isNullOrBlank()) return NnnState()
            return runCatching { json.decodeFromString<NnnStateDto>(raw).toDomain() }
                .onFailure { Log.w("NNN", "stored NNN state unreadable; starting empty") }
                .getOrDefault(NnnState())
        }
    }
}
