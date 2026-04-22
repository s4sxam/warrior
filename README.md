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
[![Internet for updates only](https://img.shields.io/badge/Internet-Updates_Only-CC0000?style=flat-square)](#)
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

The **Leaderboard** puts you against 1,050 simulated competitors across 7 world regions. They aren't bots — they behave like real people struggling with the same thing you are. See below for how this works.

<br/>

---

## The Phantom Leaderboard

1,050 simulated warriors — 150 per region — compete against you in real time. Every bot has a name, a region, a personality, and a history. The leaderboard updates every time you open the app.

### Bot Archetypes

Each bot is assigned one of six personality types that govern how they behave over time:

| Archetype | Behaviour |
|---|---|
| ⚙️ **Grinder** | Slow and steady. Rarely spikes, rarely crashes. Most consistent. |
| ⚡ **Sprinter** | Builds huge streaks fast, then collapses under psychological pressure. |
| 🔄 **Comeback Kid** | Fails often but recovers faster than anyone. High bounce-back rate. |
| 💎 **Fragile Elite** | Very high discipline but psychologically brittle — one bad week unravels them. |
| 🐉 **Underdog** | Low base discipline but capable of surprise winning runs. |
| 📉 **Plateauer** | Makes good early progress then stagnates for long stretches. |

### Simulation Algorithm (v3.0.0)

Every bot's daily clean/fail outcome is computed from this equation:

```
P(clean) = clamp(
    σ(D)              ← sigmoid discipline base
  + 0.08·ln(1+M)     ← logarithmic momentum boost
  − 0.35·(1−e^(−F·S))← plateau fatigue (streak length penalty)
  − Ps·σ(S−Sₜ)       ← psychological pressure beyond streak threshold
  + A·sin(2π·d/7 + φ)← weekly rhythm (personal weak-day sin wave)
  + Rm·0.05·e^(−0.01·Tf) ← recovery bonus after relapse (archetype-scaled)
  − Ev·0.55          ← life event penalty (active during disruption window)
, 0.05, 0.98)
```

**Where:**
- `D` = `baseDiscipline` (0.0–1.0) — intrinsic willpower, seeded per bot
- `M` = momentum (0–50) — builds on clean days, drops on relapse
- `F` = `fatigueFactor` — how fast long streaks wear the bot down
- `S` = current streak length
- `Ps` = `pressureSensitivity` — archetype-driven (Sprinter = 0.70, Grinder = 0.05)
- `Sₜ` = `streakThreshold` — streak length before pressure kicks in
- `A` = `rhythmAmplitude`, `φ` = `rhythmPhaseOffset` — personal weak-day cycle
- `Rm` = recovery multiplier from archetype config
- `Tf` = total fail days accumulated
- `Ev` = `lifeEventSeverity` — active only during a disruption window

**Life events** — every bot has a personal disruption interval (30–70 days). When triggered, a life event lasts 3–14 days and applies the `Ev` penalty for that window. This is why a top-10 bot suddenly drops 40 places and then climbs back — something happened to them.

**Weekly rhythm** — the sin wave means every bot has a personal "weak day" and "strong day" each week, producing natural organic variation that doesn't look algorithmic.

**Psychological pressure** — once a bot's streak crosses their `streakThreshold`, the pressure term activates via a sigmoid curve. Sprinters crack at day 14. Grinders don't crack until day 40+. This is why real people relapse after a long clean run.

### Dynamic Points Scoring

Points are not flat — they scale with consistency, matching the same formula for both bots and the user:

```
Clean day earned:
  base          = 2
  streak bonus  = floor(streak / 7)      (+1 per completed week)
  momentum bonus= floor(momentum / 10)   (+1 per 10 momentum)

Relapse penalty:
  base loss     = 3
  streak tax    = floor(streak / 5)      (longer streak = bigger fall)
  total capped at 12
```

A bot on a 30-day streak earns ~6 pts/day. A relapse from that streak costs ~9 pts. This creates real leaderboard drama — ranks shift every day.

### Can the user win?

Yes — but only with genuine consistency. To crack the **global top 10**, you need roughly:
- 45+ day streak, **or**
- 80+ total clean days with strong win rate

A casual user won't outrank Tier 1 Grinders. A focused user on a 60-day streak will overtake Sprinters who keep collapsing. The competition is earned, not handed to you.

### Everything is seeded

Every bot's stats, name, archetype, and daily outcomes are fully deterministic — seeded from a fixed value. The same bot always has their rough patch in week 6. Their "story" never changes between app launches, making them feel like real characters with real histories.

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
| No competition | 1,050 simulated human-like rivals across 7 regions |

The notification system doesn't fire at 2 AM. The relapse modal has a troll message. The streak ring counts up from zero every launch so you *feel* how many days you've built. These are intentional decisions, not accidents.

<br/>

---

## Built clean, built private

Your streak history, triggers, and profile are saved using Jetpack DataStore **on your own phone only** — no server, no account, no cloud.

The only time the app uses the internet is when checking for a new version on launch. It makes one read-only request to the GitHub Releases API. No data is sent. If there's no connection, the check silently skips.

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

## In-app updates

Starting from v2.2.0, Warrior 2026 checks for new versions automatically on launch and prompts you to download and install directly inside the app — no browser, no manual APK hunting.

**How it works:**
1. On launch, the app silently checks the latest GitHub Release
2. If a newer version exists and you haven't already dismissed it, a dialog appears
3. Tap **Download Update** — the APK downloads via Android's DownloadManager with a progress bar
4. Tap **Install Now** when the download completes — the system installer takes over

**What it remembers:**
- If you tap **Later**, the dialog won't appear again for that version — only when a genuinely newer version ships
- If you tap **Install Now**, the dialog won't reappear even if you cancel the system installer

**v2.3.0 update system fixes:**
- Dismiss is now persisted across app restarts — tapping "Later" actually sticks
- Retry button now properly re-downloads instead of just resetting the UI
- App no longer re-prompts for a version you already dismissed or installed
- Orphaned background downloads are cleaned up on the next launch
- Debug test button is now hidden in release builds
- APK version always matches the GitHub release tag (no more version drift)

<br/>

---

## What's coming

The core loop is done and solid. Here's what's next:

- **Home screen widget** — see your streak without opening the app
- **Multi-habit support** — track more than one thing at a time
- **Custom milestone days** — set your own targets beyond the defaults
- **Biometric lock** — fingerprint protection for your history
- **Richer charts** — win-rate trend line over the 6-month view
- **Leaderboard streaks** — show bot streak history on their profile
- **Region switching** — change your region after onboarding

The roadmap stays lean. Every feature added has to earn its place.

<br/>

---

## Why trust this?

- **Open source** — every line of code is here. Read it. Audit it. Fork it.
- **No account** — there's nothing to leak because nothing is collected
- **Internet only for updates** — one read-only GitHub API call on launch, nothing sent
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
