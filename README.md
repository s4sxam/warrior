<div align="center">
<br/>

```
```
![WARRIOR 2026](https://readme-typing-svg.demolab.com?font=Black+Ops+One&size=72&duration=1&pause=99999&color=CC0000&center=true&width=800&height=120&lines=WARRIOR+2026)
```
```
### *Stop lying to yourself. Start counting.*

<br/>

[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Android 8+](https://img.shields.io/badge/Android_8%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![No Internet](https://img.shields.io/badge/No_Internet_Required-CC0000?style=flat-square)](#)
[![MIT License](https://img.shields.io/badge/License-MIT-222222?style=flat-square)](LICENSE)

<br/>

</div>

---

<br/>

## What is this?

Warrior 2026 is a **streak tracker** — built for people fighting a bad habit and tired of apps that go easy on them.

You open the app at the end of the day. You tap **Victory** or **Relapse**. The app counts, tracks, and remembers. No social features, no coins, no premium plans. Just your streak — going up or resetting to zero.

That's it. That's the whole idea.

<br/>

---

## How the app works

Every day, you do one thing: **log your status**.

- ✅ **Victory** — tap it, confetti fires, your streak grows
- ❌ **Relapse** — tap it, optionally enter where you slipped, your streak resets

The **War Room** (home screen) shows your current streak as an animated ring, your all-time best streak, and a random discipline quote. Every time you open it, it tells you exactly where you stand — no sugarcoating.

The **Analysis** screen shows 6 months of your history as a bar chart, a consistency score from 0–100, your Warrior Rank (from *Deserter* up to *Legendary Commander*), and a list of your top relapse triggers if you've been logging them.

The **Archive** screen is a full calendar heatmap — every day you've ever logged, green or red. Scroll back months. See your patterns.

Notifications push you three times a day — morning, afternoon, evening — with messages that actually have teeth. Milestone alerts fire automatically at Day 3, 7, 14, 21, 30, 60, 90, 180, and 365.

<br/>

---

## Why it's different

Most habit apps are built to keep you opening the app. Warrior 2026 is built to keep you *off* your phone.

| Other apps | Warrior 2026 |
|---|---|
| Cloud sync, accounts, servers | Everything lives on your device only |
| Ads, premium tiers, paywalls | Free. Open source. Forever. |
| Gentle, forgiving UX | Troll messages when you relapse. Zero tolerance. |
| Streak freezes, grace periods | Relapse = zero. No exceptions. |
| Notification spam | 3 timed pushes + random motivation 9 AM–10 PM only |
| Vague history | Full calendar heatmap + trigger domain tracking |

The notification system doesn't fire at 2 AM. The relapse modal has a troll message. The streak ring counts up from zero every launch so you *feel* how many days you've built. These are intentional decisions, not accidents.

<br/>

---

## Built clean, built private

Warrior 2026 has **no internet permission** in its manifest. There is no server. There is no API call. Your history is saved using Jetpack DataStore on your own phone and nowhere else.

The architecture is standard Android MVVM:

```
Your Actions  →  ViewModel  →  Repository  →  DataStore (on-device)
                     ↓
               StateFlow  →  Compose UI  (re-renders only what changed)
```

Streaks are computed live from raw history — not stored as a number that can drift. Notifications use `AlarmManager` with exact timing and a `BootReceiver` so they survive phone restarts. The release APK runs R8 minification and resource shrinking to keep it lean.

You can **Export** your full history as a JSON file and **Import** it back anytime — useful when switching phones or just keeping a personal backup.

<br/>

---

## What's coming

The core loop is done and solid. Here's what's next:

- **Home screen widget** — see your streak without opening the app
- **Multi-habit support** — track more than one thing at a time
- **Custom milestone days** — set your own targets beyond the defaults
- **Biometric lock** — fingerprint protection for your history
- **Richer charts** — win-rate trend line over the 6-month view

The roadmap stays lean. Every feature added has to earn its place.

<br/>

---

## Why trust this?

- **Open source** — every line of code is here. Read it. Audit it. Fork it.
- **No internet** — verify it yourself with any network monitor
- **No account** — there's nothing to leak because nothing is collected
- **MIT license** — you own your build. Do whatever you want with it.
- **Built by a real user** — made by a developer who runs this on his own phone daily

<br/>

---

## Install

**From release:**
1. Download the APK from the [Releases](../../releases) tab
2. Enable *Install from unknown sources* on your device
3. Install → open → start your streak

**Build it yourself:**
```bash
git clone https://github.com/s4sxam/warrior2026.git
cd warrior2026
./gradlew assembleDebug
```
Requires Android Studio + JDK 11 + SDK 35.

<br/>

---

## Contributing

Issues, ideas, and PRs are open. If you fix a bug or build something from the roadmap, open a pull request — it'll get reviewed fast.

<br/>

---

<div align="center">

**Built by [Tanay (s3sxam) ](https://github.com/s4sxam) · India**

*One clean day at a time.*

⭐ If this helped you — star the repo. It keeps the project visible.

<br/>

`android` `kotlin` `jetpack-compose` `streak-tracker` `habit-tracker` `self-improvement`
`discipline` `productivity` `material3` `mvvm` `offline` `privacy-first` `open-source` `no-ads`

</div>
