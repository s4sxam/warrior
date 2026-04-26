package com.tanay.warrior.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

// ── CommanderVoice.kt ─────────────────────────────────────────
// Commander's Voiceline — v1.0.0
//
// Wraps Android TextToSpeech with a military commander persona.
// Low pitch (0.8f) + slow rate (0.85f) = gravitas without distortion.
//
// Usage:
//   // In your ViewModel or Activity:
//   private val commander = CommanderVoice(context)
//
//   // On streak increment:
//   commander.speakVictory(streak = 47)     // "Day 47. Holding the line."
//
//   // On relapse:
//   commander.speakRelapse()                // "You broke. Get up. War is not over."
//
//   // In onDestroy / onCleared:
//   commander.release()
//
// Lifecycle contract:
//   • CommanderVoice initialises TTS asynchronously in init.
//   • All speak calls are no-ops until TTS.SUCCESS is confirmed.
//   • release() must be called when the owner is destroyed to free
//     the underlying TTS engine. Safe to call multiple times.
//
// Victory line pool:
//   speakVictory cycles through a pool of 8 lines keyed to streak
//   milestones (day 1, 7, 30, 90+) so the voice doesn't repeat on
//   every single increment. Non-milestone days receive one of four
//   generic "holding the line" variants based on streak mod 4.
//
// Architecture:
//   Plain Kotlin class — no Compose dependency.
//   Thread-safe ready flag via @Volatile.
// ─────────────────────────────────────────────────────────────

private const val TAG              = "CommanderVoice"
private const val UTTERANCE_VICTORY = "warrior_victory"
private const val UTTERANCE_RELAPSE = "warrior_relapse"

class CommanderVoice(context: Context) {

    // ── TTS engine ────────────────────────────────────────────

    @Volatile private var ready   = false
    @Volatile private var released = false

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            Log.d(TAG, "TextToSpeech initialised.")
        } else {
            Log.w(TAG, "TextToSpeech init failed with status $status.")
        }
    }

    // ── Voice configuration ───────────────────────────────────

    // Applied on every speak call so configuration survives engine restarts.
    private fun configureTts() {
        tts.language   = Locale.US
        tts.setPitch(0.8f)            // lower than default (1.0f) — commander depth
        tts.setSpeechRate(0.85f)      // slightly slower — deliberate, not rushed
    }

    // ── Victory line pool ─────────────────────────────────────

    /**
     * Returns a voiced line appropriate for [streak].
     * Milestone days get a dedicated callout; all others rotate through
     * four generic variants based on streak mod 4.
     */
    private fun victoryLine(streak: Int): String = when (streak) {
        1    -> "Day one. The mission begins. Do not stop."
        7    -> "Day seven. One week of iron discipline. Carry on."
        14   -> "Day fourteen. Two weeks forged. The enemy watches."
        21   -> "Day twenty-one. Habit locked. You are becoming dangerous."
        30   -> "Day thirty. One month. Steel mind confirmed."
        60   -> "Day sixty. Two months. The weak have already quit."
        90   -> "Day ninety. Legend tier reached. Few ever get here."
        100  -> "Day one hundred. Absolute discipline. You are the mission."
        else -> when (streak % 4) {
            0    -> "Day $streak. Holding the line."
            1    -> "Day $streak. Still standing. Push further."
            2    -> "Day $streak. Discipline sustained. Eyes forward."
            else -> "Day $streak. The streak lives. Do not break it."
        }
    }

    // ── Public API ────────────────────────────────────────────

    /**
     * Speaks a streak-appropriate military callout.
     * No-op if TTS is not yet initialised or has been released.
     *
     * @param streak Current streak day count.
     */
    fun speakVictory(streak: Int) {
        if (!ready || released) return
        configureTts()
        val line = victoryLine(streak)
        tts.speak(line, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_VICTORY)
        Log.d(TAG, "speakVictory: \"$line\"")
    }

    /**
     * Speaks the relapse rebuke.
     * No-op if TTS is not yet initialised or has been released.
     */
    fun speakRelapse() {
        if (!ready || released) return
        configureTts()
        val line = "You broke. Get up. War is not over."
        tts.speak(line, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_RELAPSE)
        Log.d(TAG, "speakRelapse: \"$line\"")
    }

    /**
     * Stops any active speech immediately, without clearing the queue state.
     * Useful for pausing mid-sentence if the screen is backgrounded.
     */
    fun stop() {
        if (released) return
        tts.stop()
    }

    /**
     * Shuts down the TTS engine and frees all resources.
     * Must be called from onDestroy() or ViewModel.onCleared().
     * Safe to call multiple times.
     */
    fun release() {
        if (released) return
        released = true
        ready    = false
        tts.stop()
        tts.shutdown()
        Log.d(TAG, "CommanderVoice released.")
    }
}
