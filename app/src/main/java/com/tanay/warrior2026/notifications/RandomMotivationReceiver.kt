package com.tanay.warrior2026.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class RandomMotivationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs: SharedPreferences =
            context.getSharedPreferences("warrior_notif_prefs", Context.MODE_PRIVATE)

        // Track last 10 shown indices so we don't repeat
        val shownRaw = prefs.getStringSet("shown_random_ids", emptySet()) ?: emptySet()
        val shownIds = shownRaw.mapNotNull { it.toIntOrNull() }.toMutableSet()

        val pool = NotificationMessages.RANDOM
        val available = pool.indices.filter { it !in shownIds }
        val idx = if (available.isNotEmpty()) available.random() else pool.indices.random()
        val (title, body) = pool[idx]

        // Keep rolling window of last 15 to avoid repeats
        shownIds.add(idx)
        if (shownIds.size > 15) shownIds.remove(shownIds.first())

        prefs.edit()
            .putStringSet("shown_random_ids", shownIds.map { it.toString() }.toSet())
            .apply()

        // Use a unique notification ID so multiple can stack
        val notifId = 2000 + (System.currentTimeMillis() % 100).toInt()

        WarriorNotificationManager.fireNotification(
            context = context,
            notifId = notifId,
            channelId = WarriorNotificationManager.CHANNEL_MOTIVATION,
            title = title,
            body = body
        )

        // Re-schedule next random drop
        WarriorScheduler.scheduleRandomMotivation(context)
    }
}
