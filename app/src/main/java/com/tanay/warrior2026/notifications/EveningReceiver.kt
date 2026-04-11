package com.tanay.warrior2026.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class EveningReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val (title, body) = NotificationMessages.getEvening()
        WarriorNotificationManager.fireNotification(
            context = context,
            notifId = 1003,
            channelId = WarriorNotificationManager.CHANNEL_REMINDER,
            title = title,
            body = body
        )
        WarriorScheduler.scheduleEvening(context)
    }
}
