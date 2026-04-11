package com.tanay.warrior2026.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tanay.warrior2026.MainActivity
import com.tanay.warrior2026.R

object WarriorNotificationManager {

    const val CHANNEL_MOTIVATION = "warrior_motivation"
    const val CHANNEL_REMINDER = "warrior_reminder"
    const val CHANNEL_MILESTONE = "warrior_milestone"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MOTIVATION,
                    "Warrior Motivation",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Random motivational drops throughout the day"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 150, 75, 150)
                }
            )

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REMINDER,
                    "Daily Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Morning, afternoon and evening check-ins"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 200, 100, 200)
                }
            )

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MILESTONE,
                    "Streak Milestones",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Celebrate your warrior milestones"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 100, 300, 100, 300)
                }
            )
        }
    }

    fun fireNotification(
        context: Context,
        notifId: Int,
        channelId: String,
        title: String,
        body: String
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pending = PendingIntent.getActivity(
                context, notifId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notif = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(
                    if (channelId == CHANNEL_MILESTONE || channelId == CHANNEL_REMINDER)
                        NotificationCompat.PRIORITY_HIGH
                    else
                        NotificationCompat.PRIORITY_DEFAULT
                )
                .build()

            NotificationManagerCompat.from(context).notify(notifId, notif)
        } catch (e: SecurityException) {
            // Permission not granted yet — silently skip
        }
    }
}
