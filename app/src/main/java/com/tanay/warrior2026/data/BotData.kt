package com.tanay.warrior2026.data

// [UPDATE] v3.0.0: Full human-behaviour simulation rewrite
//   - BotArchetype: 6 personality types with unique probability modifiers
//   - Pressure sensitivity: long streaks psychologically break certain bots
//   - Weekly rhythm: every bot has a personal "weak day" via sin wave
//   - Life events: random disruption windows that tank performance temporarily
//   - Seeded generateBots(): discipline & fatigue now fully deterministic
//   - Revised points: streak bonuses + momentum bonuses make leaderboard dynamic

import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// ── Regions ───────────────────────────────────────────────────────────────────
enum class WarriorRegion(val displayName: String, val emoji: String) {
    ASIA("Asia", "🌏"),
    EUROPE("Europe", "🌍"),
    AFRICA("Africa", "🌍"),
    NORTH_AMERICA("North America", "🌎"),
    SOUTH_AMERICA("South America", "🌎"),
    OCEANIA("Oceania", "🌏"),
    MIDDLE_EAST("Middle East", "🌏")
}

// ── Archetypes ────────────────────────────────────────────────────────────────
//
//  GRINDER       → Slow, steady. Low pressure sensitivity. No big spikes or crashes.
//  SPRINTER      → Builds streaks fast, breaks hard. High pressure sensitivity.
//  COMEBACK_KID  → Fails often but recovers faster than anyone. High recovery bonus.
//  FRAGILE_ELITE → High discipline but psychologically brittle. Cracks under pressure.
//  UNDERDOG      → Low discipline, but occasionally pulls off surprise winning streaks.
//  PLATEAUER     → Good early progress, then long flat periods of mediocrity.
//
enum class BotArchetype {
    GRINDER,
    SPRINTER,
    COMEBACK_KID,
    FRAGILE_ELITE,
    UNDERDOG,
    PLATEAUER
}

// ── Name lists (150 per region) ───────────────────────────────────────────────
val NAMES_ASIA = listOf(
    "Rohan","Qayyum","Lokesh","Huda","Asahi","Meena","Naman","Ji-woo","Natsuki","Nabil",
    "Harsha","Kasumi","Qasim","Fumio","Darshan","Payal","Emi","Ayano","Kunal","Shirin",
    "Sayuri","Nida","Binh","Kavi","Mayur","Jian","Kanan","Pallavi","Daiki","Rakesh",
    "Kairav","Hideo","Indah","Prakash","Nithin","Kamal","Rupa","Chang","Iqbal","Kabir",
    "Diya","Naila","Hadi","Chiyo","Haruki","Samir","Farah","Akira","Mai","Shreya",
    "Seo-joon","Majid","Hikaru","Hai","Amara","Gopal","Bao","Kavya","Kazuki","Kiran",
    "Farhan","Enver","Noor","Omid","Rajan","Rania","Hasan","Ibrahim","Muneer","Pooja",
    "Jalal","Rahul","Salma","Hinata","Ramesh","Priya","Om","Ananya","Ayaan","Shahin",
    "Chau","Pranav","Manish","Azra","Kiko","Fatima","Ghazal","Kenji","Le","Li",
    "Jamal","Neha","Lin","Chaitanya","Malak","Gaurav","Safiya","Manzoor","Ismail","Latifa",
    "Nada","Mariam","Nadia","Ishaan","Lian","Maha","Saba","Masaki","Kyoko","Mahmoud",
    "Mei","Minh","Mohan","Nasir","Naveen","Parvati","Radha","Rashid","Ravi","Roshan",
    "Saad","Sahil","Sana","Sanjay","Aditya","Aarav","Amina","Anand","Arjun","Bashir",
    "Dalal","Deepak","Faiz","Faisal","Farid","Hana","Hayat","Hiroshi","Imran","Kartik",
    "Karishma","Krishna","Lakshmi","Laila","Ji-hu","Anwar","Yuki","Sohan","Tariq","Zainab"
)

val NAMES_EUROPE = listOf(
    "Stellan","Alban","Soren","Alessia","Sinead","Alistair","Silas","Amalia","Selina","Anders",
    "Saskia","Anouk","Sandor","Anton","Sabine","Anya","Rurik","Armand","Roisin","Arvid",
    "Ronan","Astrid","Rhiannon","Aurelia","Rasmus","Axel","Ragna","Bastian","Radu","Beatrix",
    "Pippa","Bogdan","Phoebe","Brando","Petra","Callum","Pavel","Carina","Otto","Casimir",
    "Otar","Casper","Orson","Celine","Orla","Chiara","Oleg","Ciaran","Oisin","Clara",
    "Noemi","Clemens","Nils","Cora","Niels","Cormac","Niamh","Cosima","Nadia","Dagmar",
    "Mireille","Damian","Mircea","Danica","Milena","Daria","Milan","Declan","Maxim","Dietrich",
    "Maura","Dimitri","Matteo","Duncan","Mathias","Eamon","Marta","Ebba","Marius","Edgar",
    "Marijn","Elara","Marek","Elina","Marcel","Elio","Malin","Elowen","Maja","Emil",
    "Magnus","Enzio","Maeva","Enya","Lyra","Ewan","Lukas","Fabian","Ludvig","Fae",
    "Lucrezia","Felix","Lucian","Fenna","Lorelei","Fiona","Lorcan","Flavia","Linnea","Flora",
    "Florian","Leonie","Freya","Leif","Gareth","Lasse","Genevieve","Laris","Gideon","Knut",
    "Gisela","Klaus","Greta","Kieran","Gunnar","Kian","Gwen","Keira","Henrik","Katja",
    "Ilaria","Kasmir","Ilse","Karsten","Imogen","Kaia","Ines","Jutta","Iona","Jovan",
    "Isadora","Jorik","Ivan","Joost","Ivar","Jeroen","Jari","Jens","Jelena","Sigrid"
)

val NAMES_AFRICA = listOf(
    "Abasi","Kabila","Abebe","Jumoke","Abena","Jomo","Abidemi","Johari","Abiodun","Jengo",
    "Abubakar","Jelani","Adama","Jamila","Adanna","Jama","Adebayo","Jalil","Adel","Jala",
    "Adetokunbo","Jaja","Adisa","Jahi","Adjoa","Jafari","Adou","Jabulani","Afia","Jabari",
    "Afolabi","Itumeleng","Agbaje","Issa","Aisha","Inna","Akachi","Imani","Akello","Ikenna",
    "Akin","Ige","Akua","Ifeanyi","Alassane","Ife","Aliou","Idrissa","Amadi","Idowu",
    "Amaia","Ibrahima","Amali","Hussein","Amani","Hisham","Amara","Hiba","Aminata","Hazim",
    "Anan","Hawa","Anele","Hasani","Anika","Hamza","Anjola","Halima","Asad","Hakim",
    "Asante","Hafsa","Asha","Habiba","Asma","Habib","Awa","Ghedi","Awiti","Ghalib",
    "Ayanda","Gacoki","Ayize","Furaha","Ayo","Folarin","Ayodele","Folami","Azibo","Femi",
    "Aziza","Fayola","Babajide","Farida","Badr","Farid","Bahiya","Farai","Bakari","Fallou",
    "Balla","Fahim","Bandile","Fadl","Barasa","Fadila","Bashir","Essien","Binta","Esi",
    "Bintou","Eshe","Boipelo","Enitan","Bolanle","Eniola","Bongiwe","Ena","Boubacar","Emeka",
    "Buhle","Ekundayo","Busisiwe","Efua","Chaka","Edem","Cheikh","Ebrima","Chidi","Ebo",
    "Chidiebere","Ebele","Chika","Diop","Chima","Dina","Chinedu","Dike","Chinwe","Diara",
    "Chizoba","Diallo","Chukwu","Desta","Dalia","Daudi","Dalili","Dara","Dakarai","Danladi"
)

val NAMES_NORTH_AMERICA = listOf(
    "Alden","Rhett","Alondra","Rex","Amory","Remy","Anahi","Reese","Ansel","Raylan",
    "Araceli","Raiden","Arden","Raegan","Ariadne","Quincy","Aris","Presley","Aspen","Porter",
    "Auden","Pierce","Avianna","Penn","Azalea","Paxton","Baelen","Orson","Bellamy","Oakley",
    "Bexley","Novalie","Blaine","Nola","Bodhi","Noa","Bowen","Nico","Bria","Nayeli",
    "Brielle","Nash","Brigham","Murphy","Brinley","Monroe","Bronson","Mireya","Cade","Miller",
    "Cael","Mila","Calloway","Merritt","Cambria","Memphis","Camden","Mckinley","Cassian","Maximiliano",
    "Colter","Maverick","Corbin","Marquis","Cortland","Maren","Creighton","Marcel","Daxton","Maliyah",
    "Dayton","Malachi","Deon","Makena","Deshaun","Maddox","Desta","Madden","Dre","Maci",
    "Easton","Luciana","Eila","Lucero","Elian","Lochlan","Ellery","Linden","Elodie","Lincoln",
    "Ember","Liana","Emory","Leona","Estavan","Lennox","Evander","Leland","Everly","Leighton",
    "Faron","Ledger","Finnegan","Lawson","Ford","Laramie","Gannon","Landry","Gatlin","Lachlan",
    "Grady","Kyler","Granger","Knox","Hadley","Kip","Haisley","Kinsley","Halston","Kian",
    "Harlow","Kensley","Hayes","Kenji","Holden","Kenia","Huxley","Kelton","Iker","Keaton",
    "Iliana","Keanu","Immanuel","Kavian","Ivey","Kason","Jace","Karsen","Jaliyah","Kamari",
    "Jamari","Kailani","Jaret","Kahlil","Jaxon","Kael","Jett","Kade","Jimena","Josue"
)

val NAMES_SOUTH_AMERICA = listOf(
    "Abelardo","Galo","Abril","Gala","Adalberto","Gabriel","Adelina","Fulgencio","Adolfo","Fresia",
    "Agostina","Fredy","Agustin","Franco","Aida","Francisca","Alba","Franca","Alcides","Fortunato",
    "Alejo","Floriana","Alfonsina","Florencia","Alfredo","Flora","Aline","Flavio","Alonso","Filomena",
    "Alvaro","Fidel","Amadeo","Fernanda","Amalia","Fermin","Amaranto","Felix","Amparo","Felipe",
    "Ana","Feliciano","Anahi","Felicia","Anderson","Federico","Andres","Fausto","Anto","Faustino",
    "Antonella","Fatima","Araceli","Facundo","Ariel","Fabricia","Arnaldo","Fabio","Arturo","Fabian",
    "Asuncion","Ezequiel","Augusto","Evelia","Aurelio","Evaristo","Ayelen","Eva","Balbina","Eusebio",
    "Baltasar","Eulalia","Barbara","Estela","Bartolome","Estefania","Bautista","Esteban","Beatriz","Esmeralda",
    "Belen","Ernesto","Benicio","Erico","Benito","Erasmo","Bernabe","Enzo","Bianca","Emma",
    "Braian","Emiliano","Bruna","Emilia","Bruno","Emanuel","Candelaria","Ema","Caridad","Elvira",
    "Carlota","Eliseo","Carmela","Elisa","Casimiro","Elida","Catalina","Elias","Cayetano","Eliana",
    "Cecilia","Elba","Celina","Efraim","Celso","Eduardo","Cesar","Edmundo","Ciro","Dulce",
    "Clara","Dora","Claudio","Domingo","Clemente","Dolores","Cleo","Dionisio","Conrado","Diogo",
    "Constanza","Dinis","Consuelo","Diego","Corina","Diana","Cristian","Desiderio","Cruz","Denis",
    "Dalia","Demetrio","Dalma","Delia","Damian","Delfina","Dania","Dario","Danilo","Dante"
)

val NAMES_OCEANIA = listOf(
    "Aarahu","Peni","Ahito","Patu","Ainsley","Parata","Alinta","Panya","Alofa","Pango",
    "Alora","Panea","Amaroo","Paka","Anahera","Paikea","Anaru","Ori","Arama","Orara",
    "Arawen","Orama","Aroha","Oma","Arorangi","Olly","Atamai","Oki","Atarangi","Nyoka",
    "Awhina","Noa","Bardo","Nita","Bindi","Nikau","Bira","Ngaire","Birani","Narelle",
    "Bodhi","Nanaia","Brolga","Nalu","Bronte","Mya","Buba","Monti","Bunya","Moana",
    "Callan","Mirri","Cardinia","Mikura","Carina","Miki","Clancy","Meria","Coen","Mau",
    "Collis","Matariki","Coreen","Matai","Corin","Maroochy","Corowa","Marise","Daku","Marika",
    "Darel","Marama","Darri","Manu","Dheran","Manawa","Efa","Manaia","Elouera","Mana",
    "Eru","Malo","Evonne","Makere","Fetia","Makani","Finlay","Maka","Finn","Maia",
    "Flynn","Mahu","Gali","Mahina","Ganya","Maaka","Gulliver","Luan","Hahona","Lowanna",
    "Hamish","Lono","Hemi","Lilo","Hine","Liko","Hinemoa","Lennie","Hohepa","Leilani",
    "Honi","Latu","Huia","Lani","Ihaia","Lachlan","Iluka","Kylie","Inia","Koura",
    "Iosefa","Koru","Irawaru","Kora","Iripa","Koby","Isla","Koa","Jalu","Kiwi",
    "Jarl","Kirra","Jiemba","Kirima","Jindalee","Kiri","Jiri","Kerehi","Jumbora","Keli",
    "Kael","Keanu","Kahu","Kauri","Kahurangi","Kane","Kairangi","Kamili","Kaleb","Kalia"
)

val NAMES_MIDDLE_EAST = listOf(
    "Zaid","Layla","Omar","Nour","Khalid","Yasmine","Tariq","Rima","Hassan","Amal",
    "Yusuf","Dina","Faris","Safa","Walid","Heba","Nasser","Rana","Bilal","Lina",
    "Karim","Maya","Adnan","Sahar","Mazen","Hind","Wael","Leen","Saif","Ghada",
    "Rami","Abeer","Jad","Samira","Amin","Noura","Samer","Ruba","Tarek","Mona",
    "Eyad","Suha","Basim","Riham","Ziad","Hala","Shadi","Ola","Nabil","Sawsan",
    "Hani","Raghad","Alaa","Duaa","Ihab","Shireen","Emad","Tahani","Akram","Asmaa",
    "Adel","Widad","Raed","Maha","Hazem","Lujain","Raouf","Nada","Osama","Rasha",
    "Waseem","Lama","Jawad","Inas","Bashir","Roua","Munir","Manal","Naeem","Hana",
    "Fuad","Lara","Ihsan","Rawan","Ayman","Tala","Hamid","Salwa","Fawzi","Dalia",
    "Lutfi","Wafa","Ghassan","Abla","Marwan","Reham","Rafiq","Bushra","Nidal","Arwa",
    "Samir","Asil","Bassel","Randa","Suhail","Zeina","Karam","Nisreen","Raif","Sabah",
    "Fadi","Tamara","Elias","Nawal","Charbel","Hoda","Ramzi","Fatma","Toufic","Suad",
    "Habib","Wissam","Naji","Siham","Kassem","Roula","Rabih","Mira","Elie","Lobna",
    "Ziyad","Ahlam","Amir","Ibtisam","Salam","Haneen","Nizar","Majd","Taim","Ghalia",
    "Khaled","Rajaa","Ahmad","Sousou","Mohamad","Houda","Wassim","Sirine","Celine","Jinan"
)

fun namesForRegion(region: WarriorRegion): List<String> = when (region) {
    WarriorRegion.ASIA          -> NAMES_ASIA
    WarriorRegion.EUROPE        -> NAMES_EUROPE
    WarriorRegion.AFRICA        -> NAMES_AFRICA
    WarriorRegion.NORTH_AMERICA -> NAMES_NORTH_AMERICA
    WarriorRegion.SOUTH_AMERICA -> NAMES_SOUTH_AMERICA
    WarriorRegion.OCEANIA       -> NAMES_OCEANIA
    WarriorRegion.MIDDLE_EAST   -> NAMES_MIDDLE_EAST
}

// ── Bot profile ───────────────────────────────────────────────────────────────
@Serializable
data class BotProfile(
    // Identity
    val id: Int,
    val name: String,
    val region: String,             // WarriorRegion.name
    val seed: Long,

    // Core discipline
    val baseDiscipline: Double,     // 0.0–1.0
    val fatigueFactor: Double,      // 0.003–0.020

    // Archetype
    val archetype: String,          // BotArchetype.name (serialization-safe)
    val pressureSensitivity: Double,// 0.0–1.0: how much long streaks psyche them out
    val streakThreshold: Int,       // streak length before pressure kicks in

    // Weekly rhythm — personal "weak day" via sin wave
    val rhythmAmplitude: Double,    // 0.0–0.10: max swing from weekly cycle
    val rhythmPhaseOffset: Double,  // 0.0–6.28: shifts which day of week is weakest

    // Life events — random disruption windows
    val lifeEventInterval: Int,     // avg days between disruptions (30–70)
    val lifeEventSeverity: Double,  // 0.0–1.0: how hard events hit this bot

    // Runtime state
    var points: Int = 0,
    var currentStreak: Int = 0,
    var momentum: Double = 0.0,
    var totalCleanDays: Int = 0,
    var totalFailDays: Int = 0,
    var inLifeEvent: Boolean = false,
    var lifeEventDaysLeft: Int = 0,
    var lastSimulatedDay: String = "",

    // [NEW v3.2.0] The date bots were first generated (= app install date).
    // Simulation and heatmap never show data before this date.
    // Format: ISO_LOCAL_DATE. Blank on legacy bots = no restriction.
    val simulationStartDate: String = ""
)

// ── Archetype modifiers ───────────────────────────────────────────────────────
//
// Each archetype adjusts specific parameters at generation time:
//
//  GRINDER       pressureSensitivity ≈ 0.05  streakThreshold ≈ 40  rhythmAmplitude ≈ 0.02
//  SPRINTER      pressureSensitivity ≈ 0.70  streakThreshold ≈ 14  rhythmAmplitude ≈ 0.05
//  COMEBACK_KID  pressureSensitivity ≈ 0.30  streakThreshold ≈ 21  recoveryBonus ×2.0
//  FRAGILE_ELITE pressureSensitivity ≈ 0.85  streakThreshold ≈ 10  (high discipline only)
//  UNDERDOG      pressureSensitivity ≈ 0.20  streakThreshold ≈ 30  (low discipline only)
//  PLATEAUER     pressureSensitivity ≈ 0.15  rhythmAmplitude ≈ 0.08 (flat momentum gain)
//
data class ArchetypeConfig(
    val pressureSensitivity: Double,
    val streakThreshold: Int,
    val rhythmAmplitudeBonus: Double,
    val recoveryMultiplier: Double,   // multiplied into the recovery bonus
    val lifeEventSeverityBonus: Double
)

fun archetypeConfig(archetype: BotArchetype): ArchetypeConfig = when (archetype) {
    BotArchetype.GRINDER       -> ArchetypeConfig(0.05, 40, 0.00, 1.0, 0.0)
    BotArchetype.SPRINTER      -> ArchetypeConfig(0.70, 14, 0.03, 0.8, 0.1)
    BotArchetype.COMEBACK_KID  -> ArchetypeConfig(0.30, 21, 0.02, 2.5, 0.0)
    BotArchetype.FRAGILE_ELITE -> ArchetypeConfig(0.85, 10, 0.01, 0.6, 0.3)
    BotArchetype.UNDERDOG      -> ArchetypeConfig(0.20, 30, 0.04, 1.2, 0.1)
    BotArchetype.PLATEAUER     -> ArchetypeConfig(0.15, 25, 0.08, 0.9, 0.0)
}

// ── Survival Probability ──────────────────────────────────────────────────────
//
//  P(clean) = clamp(
//      σ(D)                       ← sigmoid discipline base
//    + 0.08·ln(1+M)              ← logarithmic momentum boost
//    − fatigue_term               ← plateau fatigue from long streaks
//    − pressure_term              ← psychological pressure (archetype-driven)
//    + rhythm_term                ← weekly sin-wave cycle (personal weak day)
//    + recovery_term              ← bounce-back after relapse
//    + life_event_penalty         ← random disruption window
//  , 0.05, 0.98)
//
//  fatigue_term  = 0.35·(1−e^(−F·S))          — same as before
//  pressure_term = Ps·σ(S−Sₜ)                 — NEW: sigmoid beyond threshold
//  rhythm_term   = A·sin(2π·dow/7 + φ)        — NEW: weekly cycle
//  recovery_term = Rm·0.05·e^(−0.01·Tf)       — modified by archetype multiplier
//  life_event_penalty = severity·0.55          — NEW: active only during event window
//
fun survivalProbability(
    bot: BotProfile,
    dayOfWeek: Int = 0,           // 0=Mon … 6=Sun
    lifeEventActive: Boolean = false
): Double {
    val archetype = runCatching { BotArchetype.valueOf(bot.archetype) }
        .getOrDefault(BotArchetype.GRINDER)
    val cfg = archetypeConfig(archetype)

    // 1. Sigmoid discipline base
    val sigmoidBase = 1.0 / (1.0 + exp(-10.0 * (bot.baseDiscipline - 0.5)))

    // 2. Logarithmic momentum boost
    val momentumBoost = 0.08 * ln(1.0 + bot.momentum)

    // 3. Plateau fatigue penalty
    val fatiguePenalty = (1.0 - exp(-bot.fatigueFactor * bot.currentStreak)) * 0.35

    // 4. Psychological pressure beyond threshold (archetype-driven)
    //    σ(S − Sₜ) → 0 when S << Sₜ, rises to 1 when S >> Sₜ
    val pressureTerm = if (bot.currentStreak > bot.streakThreshold) {
        val x = (bot.currentStreak - bot.streakThreshold).toDouble()
        bot.pressureSensitivity * (1.0 / (1.0 + exp(-0.3 * (x - 5.0))))
    } else 0.0

    // 5. Weekly rhythm — personal weak-day sin wave
    //    Positive peak = strong day, Negative peak = weak day
    val rhythmTerm = bot.rhythmAmplitude * sin(
        2.0 * Math.PI * dayOfWeek / 7.0 + bot.rhythmPhaseOffset
    )

    // 6. Recovery bonus after relapse (fades with repeated failures)
    val recoveryTerm = if (bot.currentStreak == 0)
        cfg.recoveryMultiplier * 0.05 * exp(-bot.totalFailDays * 0.01)
    else 0.0

    // 7. Life event penalty (active only during event window)
    val lifeEventPenalty = if (lifeEventActive)
        bot.lifeEventSeverity * 0.55
    else 0.0

    val raw = sigmoidBase + momentumBoost - fatiguePenalty - pressureTerm +
              rhythmTerm + recoveryTerm - lifeEventPenalty

    return raw.coerceIn(0.05, 0.98)
}

// ── Points scoring ────────────────────────────────────────────────────────────
//
//  Clean day earned:
//    base       = 2
//    streakBonus= floor(streak / 7)     (+1 per completed week of streak)
//    momentumBonus = floor(momentum / 10) (+1 per 10 momentum)
//
//  Relapse loss:
//    base_loss  = 3
//    streakTax  = floor(streak_before / 5)  (longer streak = bigger fall from grace)
//    total_loss capped at 12
//
fun pointsForCleanDay(streak: Int, momentum: Double): Int {
    val base         = 2
    val streakBonus  = floor(streak / 7.0).toInt()
    val momentumBonus = floor(momentum / 10.0).toInt()
    return base + streakBonus + momentumBonus
}

fun pointsLostOnRelapse(streakBefore: Int): Int {
    val baseLoss   = 3
    val streakTax  = floor(streakBefore / 5.0).toInt()
    return minOf(baseLoss + streakTax, 12)
}

// ── Tier — derived from baseDiscipline ───────────────────────────────────────
fun tierOf(bot: BotProfile): String = when {
    bot.baseDiscipline >= 0.96 -> "1 — Elite"
    bot.baseDiscipline >= 0.91 -> "2 — Advanced"
    bot.baseDiscipline >= 0.84 -> "3 — Intermediate"
    bot.baseDiscipline >= 0.72 -> "4 — Developing"
    else                       -> "5 — Struggling"
}

// ── Procedural calendar generation ───────────────────────────────────────────
fun generateBotCalendar(bot: BotProfile, days: Int = 365): Map<String, Boolean> {
    val rng       = Random(bot.seed)
    val result    = LinkedHashMap<String, Boolean>()
    val startDate = java.time.LocalDate.now().minusDays(days.toLong() - 1)
    var tempStreak   = 0
    var tempMomentum = 0.0
    var lifeEventDaysLeft = 0

    repeat(days) { dayIdx ->
        val date       = startDate.plusDays(dayIdx.toLong())
        val key        = date.format(DATE_FORMATTER)
        val dayOfWeek  = date.dayOfWeek.value - 1  // 0=Mon … 6=Sun

        // Life event logic
        if (lifeEventDaysLeft > 0) {
            lifeEventDaysLeft--
        } else if (dayIdx > 0 && rng.nextInt(bot.lifeEventInterval) == 0) {
            lifeEventDaysLeft = rng.nextInt(3, 15)
        }
        val lifeEventActive = lifeEventDaysLeft > 0

        val tempBot = bot.copy(
            currentStreak = tempStreak,
            momentum      = tempMomentum,
            inLifeEvent   = lifeEventActive,
            lifeEventDaysLeft = lifeEventDaysLeft
        )
        val prob  = survivalProbability(tempBot, dayOfWeek, lifeEventActive)
        val clean = rng.nextDouble() < prob
        result[key] = clean

        if (clean) {
            tempStreak++
            tempMomentum = min(tempMomentum + 1.0, 50.0)
        } else {
            tempStreak   = 0
            tempMomentum = maxOf(tempMomentum - 3.0, 0.0)
        }
    }
    return result
}

// ── Bot generation (fully seeded — deterministic) ─────────────────────────────
fun generateBots(): List<BotProfile> {
    val bots     = mutableListOf<BotProfile>()
    var globalId = 0

    // Archetype distribution per tier (10 + 20 + 30 + 40 + 50 = 150 per region)
    // Tier 1 Elite (i < 10):       mostly GRINDER or FRAGILE_ELITE
    // Tier 2 Advanced (i < 30):    GRINDER, SPRINTER, COMEBACK_KID
    // Tier 3 Intermediate (i < 60):all archetypes
    // Tier 4 Developing (i < 100): UNDERDOG, COMEBACK_KID, PLATEAUER, SPRINTER
    // Tier 5 Struggling (i < 150): UNDERDOG, PLATEAUER, SPRINTER

    val tier1Archetypes  = listOf(BotArchetype.GRINDER, BotArchetype.FRAGILE_ELITE)
    val tier2Archetypes  = listOf(BotArchetype.GRINDER, BotArchetype.SPRINTER, BotArchetype.COMEBACK_KID)
    val tier3Archetypes  = BotArchetype.entries.toList()
    val tier4Archetypes  = listOf(BotArchetype.UNDERDOG, BotArchetype.COMEBACK_KID, BotArchetype.PLATEAUER, BotArchetype.SPRINTER)
    val tier5Archetypes  = listOf(BotArchetype.UNDERDOG, BotArchetype.PLATEAUER, BotArchetype.SPRINTER)

    WarriorRegion.entries.forEachIndexed { rIdx, region ->
        // Seeded shuffle so names are always the same for a given region
        val names = namesForRegion(region).shuffled(Random(rIdx.toLong() * 7919L))

        repeat(150) { i ->
            // Fully seeded RNG per bot — same bot always has same stats
            val botSeed = (rIdx * 150L + i) * 31337L
            val rng     = Random(botSeed)

            val (dBase, fFactor, archetypePool) = when {
                i < 10  -> Triple(
                    rng.nextDouble(0.96, 0.99),
                    rng.nextDouble(0.003, 0.005),
                    tier1Archetypes
                )
                i < 30  -> Triple(
                    rng.nextDouble(0.91, 0.96),
                    rng.nextDouble(0.005, 0.008),
                    tier2Archetypes
                )
                i < 60  -> Triple(
                    rng.nextDouble(0.84, 0.91),
                    rng.nextDouble(0.007, 0.011),
                    tier3Archetypes
                )
                i < 100 -> Triple(
                    rng.nextDouble(0.72, 0.84),
                    rng.nextDouble(0.009, 0.014),
                    tier4Archetypes
                )
                else    -> Triple(
                    rng.nextDouble(0.40, 0.72),
                    rng.nextDouble(0.012, 0.020),
                    tier5Archetypes
                )
            }

            val archetype = archetypePool[rng.nextInt(archetypePool.size)]
            val cfg       = archetypeConfig(archetype)

            bots.add(
                BotProfile(
                    id                   = globalId++,
                    name                 = names.getOrElse(i) { "Warrior${globalId}" },
                    region               = region.name,
                    seed                 = botSeed,
                    baseDiscipline       = dBase,
                    fatigueFactor        = fFactor,
                    archetype            = archetype.name,
                    pressureSensitivity  = cfg.pressureSensitivity +
                                          rng.nextDouble(-0.05, 0.05).coerceIn(-cfg.pressureSensitivity, 0.2),
                    streakThreshold      = cfg.streakThreshold + rng.nextInt(-3, 4),
                    rhythmAmplitude      = rng.nextDouble(0.01, 0.06) + cfg.rhythmAmplitudeBonus,
                    rhythmPhaseOffset    = rng.nextDouble(0.0, 2.0 * Math.PI),
                    lifeEventInterval    = rng.nextInt(30, 71),
                    lifeEventSeverity    = rng.nextDouble(0.2, 0.7) + cfg.lifeEventSeverityBonus
                )
            )
        }
    }
    return bots
}