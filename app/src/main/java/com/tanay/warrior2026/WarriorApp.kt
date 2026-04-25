package com.tanay.warrior

import android.app.Application
import com.tanay.warrior.notifications.WarriorNotificationManager
import com.tanay.warrior.notifications.WarriorScheduler

class WarriorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WarriorNotificationManager.createChannels(this)
        WarriorScheduler.scheduleAll(this)
    }
}
