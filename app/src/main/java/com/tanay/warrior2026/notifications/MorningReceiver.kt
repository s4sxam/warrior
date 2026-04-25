package com.tanay.warrior.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tanay.warrior.notifications.NotificationMessages
import com.tanay.warrior.notifications.WarriorNotificationManager

class MorningReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val (title, body) = NotificationMessages.getMorning()
        WarriorNotificationManager.fireNotification(
            context = context,
            notifId = 1001,
            channelId = WarriorNotificationManager.CHANNEL_REMINDER,
            title = title,
            body = body
        )
        // Re-schedule next morning
        WarriorScheduler.scheduleMorning(context)
    }
}
