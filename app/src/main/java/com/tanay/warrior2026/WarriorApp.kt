package com.tanay.warrior2026

import android.app.Application
import com.tanay.warrior2026.notifications.WarriorNotificationManager
import com.tanay.warrior2026.notifications.WarriorScheduler

class WarriorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WarriorNotificationManager.createChannels(this)
        WarriorScheduler.scheduleAll(this)
    }
}
