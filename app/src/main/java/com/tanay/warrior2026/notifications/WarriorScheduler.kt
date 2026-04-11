package com.tanay.warrior2026.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import kotlin.random.Random

object WarriorScheduler {

    fun scheduleAll(context: Context) {
        if (!isScheduled(context, 101, MorningReceiver::class.java))   scheduleMorning(context)
        if (!isScheduled(context, 102, AfternoonReceiver::class.java)) scheduleAfternoon(context)
        if (!isScheduled(context, 103, EveningReceiver::class.java))   scheduleEvening(context)
        if (!isScheduled(context, 200, RandomMotivationReceiver::class.java)) scheduleRandomMotivation(context)
    }

    private fun <T : Any> isScheduled(context: Context, requestCode: Int, receiverClass: Class<T>): Boolean {
        val intent = Intent(context, receiverClass)
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pending != null
    }

    fun scheduleMorning(context: Context) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, Random.nextInt(0, 59)) // FIX: was 0..44 OK but explicit max 59
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        schedule(context, cal.timeInMillis, 101, MorningReceiver::class.java)
    }

    fun scheduleAfternoon(context: Context) {
        // FIX: was Random.nextInt(0, 90) — minute must be 0–59
        // Afternoon window is 13:00–14:59 → vary hour too
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 13 + Random.nextInt(0, 2)) // 13 or 14
            set(Calendar.MINUTE, Random.nextInt(0, 59))
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        schedule(context, cal.timeInMillis, 102, AfternoonReceiver::class.java)
    }

    fun scheduleEvening(context: Context) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, Random.nextInt(0, 59))
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        schedule(context, cal.timeInMillis, 103, EveningReceiver::class.java)
    }

    fun scheduleRandomMotivation(context: Context) {
        val delayHours   = Random.nextInt(3, 9)
        val delayMinutes = Random.nextInt(0, 59)
        val delayMs      = ((delayHours * 60L + delayMinutes) * 60_000L)
        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis() + delayMs
            val hour = get(Calendar.HOUR_OF_DAY)
            when {
                hour < 9  -> { set(Calendar.HOUR_OF_DAY, 9 + Random.nextInt(0, 3)); set(Calendar.MINUTE, Random.nextInt(0, 59)) }
                hour >= 23 -> {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9 + Random.nextInt(0, 2))
                    set(Calendar.MINUTE, Random.nextInt(0, 59))
                }
            }
        }
        schedule(context, cal.timeInMillis, 200, RandomMotivationReceiver::class.java)
    }

    fun fireMilestoneNow(context: Context, streak: Int) {
        val intent = Intent(context, MilestoneReceiver::class.java).apply {
            putExtra("streak", streak)
            setPackage(context.packageName) // restrict broadcast to own package
        }
        context.sendBroadcast(intent)
    }

    private fun <T : Any> schedule(
        context: Context, triggerAtMillis: Long,
        requestCode: Int, receiverClass: Class<T>
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, receiverClass)
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                } else {
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