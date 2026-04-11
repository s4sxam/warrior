package com.tanay.warrior2026.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MilestoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val streak = intent.getIntExtra("streak", 0)
        val (baseTitle, baseBody) = NotificationMessages.getMilestone()
        val title = "$baseTitle 🔥 $streak Days!"
        val body = buildMilestoneBody(streak)

        WarriorNotificationManager.fireNotification(
            context = context,
            notifId = 3000 + streak,
            channelId = WarriorNotificationManager.CHANNEL_MILESTONE,
            title = title,
            body = body
        )
    }

    private fun buildMilestoneBody(streak: Int): String = when {
        streak >= 365 -> "ONE FULL YEAR. You are not human. You are LEGEND. 🏆"
        streak >= 180 -> "180 days. Half a year of pure dominance. The world respects you."
        streak >= 90  -> "90 days. A full season of clarity. Your mind is rebuilt."
        streak >= 60  -> "60 days! Two months clean. You are proof it is possible."
        streak >= 30  -> "30 days. One month warrior. Most people can't say this."
        streak >= 21  -> "21 days. New habits are forming in your brain RIGHT NOW."
        streak >= 14  -> "Two weeks strong. You've broken the hardest barrier."
        streak >= 7   -> "One full week clean. Seven days of real discipline. Keep going."
        streak >= 3   -> "3 days! The restart is done. Now build the momentum."
        else          -> "Day $streak. Every day clean is a day you WIN."
    }
}
