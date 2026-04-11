package com.tanay.warrior2026.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import kotlin.random.Random

object WarriorScheduler {

    // ──────────────────────────────────────────────
    //  PUBLIC ENTRY POINTS
    // ──────────────────────────────────────────────

    fun scheduleAll(context: Context) {
        scheduleMorning(context)
        scheduleAfternoon(context)
        scheduleEvening(context)
        scheduleRandomMotivation(context)
    }

    // ──────────────────────────────────────────────
    //  MORNING  — 6:00 AM  ± 0–45 min random offset
    // ──────────────────────────────────────────────
    fun scheduleMorning(context: Context) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, Random.nextInt(0, 45))
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        schedule(
            context = context,
            triggerAtMillis = cal.timeInMillis,
            requestCode = 101,
            receiverClass = MorningReceiver::class.java
        )
    }

    // ──────────────────────────────────────────────
    //  AFTERNOON  — 1:00 PM  ± 0–90 min random
    // ──────────────────────────────────────────────
    fun scheduleAfternoon(context: Context) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, Random.nextInt(0, 90))
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        schedule(
            context = context,
            triggerAtMillis = cal.timeInMillis,
            requestCode = 102,
            receiverClass = AfternoonReceiver::class.java
        )
    }

    // ──────────────────────────────────────────────
    //  EVENING  — 8:00 PM  ± 0–60 min random
    // ──────────────────────────────────────────────
    fun scheduleEvening(context: Context) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, Random.nextInt(0, 60))
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        schedule(
            context = context,
            triggerAtMillis = cal.timeInMillis,
            requestCode = 103,
            receiverClass = EveningReceiver::class.java
        )
    }

    // ──────────────────────────────────────────────
    //  RANDOM MOTIVATION — 1 to 3 per day
    //  Fires at a completely random time in the
    //  waking window (9 AM – 11 PM) and reschedules
    //  itself with a new random delay (3–8 hours).
    // ──────────────────────────────────────────────
    fun scheduleRandomMotivation(context: Context) {
        // Pick a delay between 3 and 8 hours from now (in millis)
        val delayHours = Random.nextInt(3, 9)
        val delayMinutes = Random.nextInt(0, 60)
        val delayMs = ((delayHours * 60 + delayMinutes) * 60 * 1000).toLong()

        val triggerAt = System.currentTimeMillis() + delayMs

        // Clamp to waking hours: 9 AM – 11 PM
        val cal = Calendar.getInstance().apply { timeInMillis = triggerAt }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        when {
            hour < 9  -> cal.set(Calendar.HOUR_OF_DAY, 9 + Random.nextInt(0, 3))
            hour >= 23 -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 9 + Random.nextInt(0, 2))
                cal.set(Calendar.MINUTE, Random.nextInt(0, 60))
            }
        }

        schedule(
            context = context,
            triggerAtMillis = cal.timeInMillis,
            requestCode = 200,
            receiverClass = RandomMotivationReceiver::class.java
        )
    }

    // ──────────────────────────────────────────────
    //  MILESTONE — called from ViewModel when
    //  streak hits a checkpoint
    // ──────────────────────────────────────────────
    fun fireMilestoneNow(context: Context, streak: Int) {
        val intent = Intent(context, MilestoneReceiver::class.java).apply {
            putExtra("streak", streak)
        }
        context.sendBroadcast(intent)
    }

    // ──────────────────────────────────────────────
    //  INTERNAL HELPER
    // ──────────────────────────────────────────────
    private fun <T : Any> schedule(
        context: Context,
        triggerAtMillis: Long,
        requestCode: Int,
        receiverClass: Class<T>
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, receiverClass)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                } else {
                    // Fallback to inexact if exact permission not granted
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                }
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
        } catch (e: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }
}
