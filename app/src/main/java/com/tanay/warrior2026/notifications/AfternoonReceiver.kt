package com.tanay.warrior.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AfternoonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val (title, body) = NotificationMessages.getAfternoon()
        WarriorNotificationManager.fireNotification(
            context = context,
            notifId = 1002,
            channelId = WarriorNotificationManager.CHANNEL_REMINDER,
            title = title,
            body = body
        )
        WarriorScheduler.scheduleAfternoon(context)
    }
}
