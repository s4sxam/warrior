package com.tanay.warrior.notifications

/**
 * All notification messages for Warrior.
 * Each entry is a Pair(title, body).
 * Kept in a large pool so repeats are rare.
 */
object NotificationMessages {

    val MORNING = listOf(
        Pair("☀️ Rise, Warrior.", "The weak are still asleep. You're already winning."),
        Pair("🔥 New Day. New War.", "Yesterday is dead. Today you dominate."),
        Pair("⚔️ Your streak starts NOW.", "Not at noon. Not after breakfast. NOW."),
        Pair("💀 The enemy doesn't sleep.", "But you're awake. That's already victory."),
        Pair("🌅 Commander Mode: ACTIVE", "The battlefield is open. Do not waste this morning."),
        Pair("🏆 Day 1 or Day ${(10..999).random()}?", "Either way — earn it today. Log your status tonight."),
        Pair("⚡ MORNING BRIEFING", "Mission: Stay clean. Duration: 24 hours. Begin."),
        Pair("💪 Ancestors are watching.", "Make them proud before 9 AM."),
        Pair("🎯 One goal today.", "Don't fall. Everything else is secondary."),
        Pair("🔱 Warriors don't negotiate with weakness.", "You wake up. You decide. You win."),
    )

    val AFTERNOON = listOf(
        Pair("☕ Midday check-in.", "Still standing? Good. Keep going."),
        Pair("⚡ Danger hour approaching.", "Stay busy. Idle hands lose wars."),
        Pair("🛡️ Half the day done.", "Don't throw it away in the next 10 minutes."),
        Pair("💀 The enemy attacks at noon.", "Boredom is the weapon. Destroy it with action."),
        Pair("🔥 Still in the fight?", "Good. The next 6 hours are all that matters."),
        Pair("🏋️ Drop and do 20.", "If you're thinking about failing — do pushups instead."),
        Pair("⚔️ Commander status check.", "Are you busy? If not — GET BUSY."),
        Pair("🎯 Afternoon rule:", "If you're bored, you're vulnerable. Move. Do something."),
        Pair("💪 Discipline is a muscle.", "It gets stronger every hour you don't give in."),
        Pair("🧠 Your brain is lying to you.", "'Just this once' is how streaks die. Ignore it."),
    )

    val EVENING = listOf(
        Pair("🌆 Evening is dangerous.", "Log your victory NOW before it becomes a loss."),
        Pair("🔥 Almost there, Warrior.", "Don't collapse at the finish line."),
        Pair("💀 The hardest hours are here.", "Hold the line until midnight. That's it."),
        Pair("⚡ Did you log today?", "Open the app. Tap victory. Sleep like a king."),
        Pair("🏆 One more hour of clean.", "Then one more. That's how streaks are built."),
        Pair("🛡️ Evening defense protocol.", "Phone down. Cold water. 50 pushups. Repeat."),
        Pair("🌙 Night is coming.", "Warriors don't fall in the dark."),
        Pair("⚔️ You've survived the day.", "Don't throw it away in the last 3 hours."),
        Pair("💪 Log it before you sleep.", "Your streak is counting on you."),
        Pair("🔱 This is your moment.", "The weak fail here. You are not weak."),
    )

    val RANDOM = listOf(
        Pair("💀 Skull reminder:", "Every relapse resets EVERYTHING. Is it worth it?"),
        Pair("🔥 Pain is temporary.", "Regret is forever. Choose wisely."),
        Pair("⚡ Your future self is watching.", "He's either proud or ashamed. You decide."),
        Pair("🏆 Streak check.", "Every day clean is a day your enemy loses."),
        Pair("💪 Nobody is coming to save you.", "Only you can hold the line."),
        Pair("🎯 Real talk:", "Cheap dopamine destroys real happiness. Stop."),
        Pair("⚔️ The algorithm wants you weak.", "Prove it wrong today."),
        Pair("🛡️ Warrior law #1:", "If you have to hide it — you already know it's wrong."),
        Pair("🔱 What are you building?", "Every clean day is a brick. Every relapse is a bomb."),
        Pair("🌙 Sleep clean tonight.", "Tomorrow you wake up stronger. It compounds."),
        Pair("💀 Statistical fact:", "Most relapses happen between 9PM and 1AM. Be ready."),
        Pair("⚡ Close the browser.", "You know what you're about to do. Don't."),
        Pair("🏋️ 50 pushups challenge.", "Right now. No excuses. Go."),
        Pair("🔥 Think about your streak.", "Is 5 minutes of weakness worth losing it all?"),
        Pair("🎯 Commander override:", "Drink water. Go outside. Call someone. Anything but that."),
        Pair("💪 Discipline = freedom.", "The irony is — control gives you more life, not less."),
        Pair("⚔️ You're in a war.", "Treat it like one. Wars aren't won on 'easy' days."),
        Pair("🌅 New perspective:", "Imagine showing your future son your daily logs. Would you be proud?"),
        Pair("🛡️ Block it. Delete it. Walk away.", "The three steps that save your streak every time."),
        Pair("💀 Your brain is not your friend right now.", "It wants the easy path. Deny it."),
        Pair("⚡ Real warriors do hard things.", "Especially when they don't want to."),
        Pair("🏆 This is day ${(1..365).random()}.", "Make it count. Log it tonight."),
        Pair("🔱 The enemy uses boredom.", "Destroy boredom with purpose."),
        Pair("🔥 What's your WHY?", "Remember it. Right now. Hold it tight."),
        Pair("💪 Cold shower. Right now.", "3 minutes. Transforms your entire headspace."),
        Pair("🎯 The streak is the mission.", "Everything else is noise."),
        Pair("⚔️ You've come too far to fail today.", "Every day you've logged is proof you can do this."),
        Pair("🌙 One decision away.", "You're always one decision away from winning or losing."),
        Pair("💀 Log your status.", "Open the app. Tap clean. Close it. Move on."),
        Pair("⚡ Reminder:", "No one respects a man with no self-control. Including yourself."),
    )

    val MILESTONE = listOf(
        Pair("🏆 MILESTONE UNLOCKED!", "You're on a serious streak. Don't let it die here."),
        Pair("⚡ STREAK ALERT!", "You've built something real. Guard it with your life."),
        Pair("🔥 WARRIOR STATUS ELEVATED", "Your streak is proof. Keep the fire burning."),
        Pair("💀 MOMENTUM WARNING", "Big streaks attract big temptations. Stay sharp."),
        Pair("🛡️ LEGENDARY TERRITORY", "Few make it this far. You're one of them. Keep going."),
    )

    fun getRandomFromPool(lastShownIds: Set<Int>): Pair<String, String> {
        val all = MORNING + AFTERNOON + EVENING + RANDOM + MILESTONE
        val available = all.filterIndexed { index, _ -> index !in lastShownIds }
        return if (available.isNotEmpty()) available.random()
        else all.random()
    }

    fun getMorning() = MORNING.random()
    fun getAfternoon() = AFTERNOON.random()
    fun getEvening() = EVENING.random()
    fun getRandom() = RANDOM.random()
    fun getMilestone() = MILESTONE.random()
}
