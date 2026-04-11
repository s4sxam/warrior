# Warrior 2026 — Android App

## Setup Instructions

### 1. Open in Android Studio
- Open Android Studio → **File → Open** → select this folder
- Android Studio will auto-download Gradle and all dependencies on first sync

### 2. gradle-wrapper.jar (IMPORTANT)
The `gradle/wrapper/gradle-wrapper.jar` is not included (it's a binary).
Android Studio will generate it automatically. If it doesn't:

```bash
# Run this once inside the project folder:
gradle wrapper --gradle-version=8.7
```

Or download manually:
https://services.gradle.org/distributions/gradle-8.7-bin.zip

### 3. Build APK
- In Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
- APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### 4. Notification Permissions
On Android 13+ the app will request POST_NOTIFICATIONS permission on first launch.
Grant it to enable all notifications.

## Notification System
| Type | Time | Channel |
|------|------|---------|
| ☀️ Morning | 6:00–6:45 AM (random) | Daily Reminders |
| ☕ Afternoon | 1:00–2:30 PM (random) | Daily Reminders |
| 🌆 Evening | 8:00–9:00 PM (random) | Daily Reminders |
| ⚡ Random Motivation | 3–8 hrs apart, 9AM–11PM | Warrior Motivation |
| 🏆 Milestone | Streak days 3,7,14,21,30,60,90,180,365 | Streak Milestones |

All notifications survive device reboot (BootReceiver reschedules them).
