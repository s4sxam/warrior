package com.tanay.warrior2026.data

// ── [NEW] BotData.kt ──────────────────────────────────────────────────────────
// Holds all data models and name lists for the Phantom Leaderboard system.
// 1,050 bots total: 150 per region × 7 regions.
// [UPDATE] v2.2.0: A+ survival probability equation — sigmoid + log momentum
//                  + plateau fatigue + recovery bonus. Tier from discipline value.

import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
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

// ── Bot model ─────────────────────────────────────────────────────────────────
@Serializable
data class BotProfile(
    val id: Int,                    // 0..1049 (region * 150 + index)
    val name: String,
    val region: String,             // WarriorRegion.name
    val baseDiscipline: Double,     // 0.0–1.0
    val fatigueFactor: Double,      // how fast fatigue builds
    val seed: Long,                 // deterministic random seed for calendar generation
    var points: Int = 0,
    var currentStreak: Int = 0,
    var momentum: Double = 0.0,
    var totalCleanDays: Int = 0,
    var totalFailDays: Int = 0,
    var lastSimulatedDay: String = ""
)

// ── Momentum & Fatigue Algorithm ─────────────────────────────────────────────
//
// P(survival) = D_base + (M × W_m) − e^(S × F)
//
// where:
//   D_base = baseDiscipline (0.0–1.0)
//   M      = momentum (0–50), W_m = 0.005
//   S      = currentStreak
//   F      = fatigueFactor (0.003–0.020)
//
// Clamped to [0.05, 0.98]
//
fun survivalProbability(bot: BotProfile): Double {
    val base    = bot.baseDiscipline + (bot.momentum * 0.005)
    val fatigue = exp(bot.fatigueFactor * bot.currentStreak)
    return (base - fatigue).coerceIn(0.05, 0.98)
}

// ── Tier — derived from actual discipline value, not arbitrary ID ─────────────
// Matches the generation ranges in generateBots() exactly:
//   Tier 1 Elite       → D ∈ [0.96, 0.99)
//   Tier 2 Advanced    → D ∈ [0.91, 0.96)
//   Tier 3 Intermediate→ D ∈ [0.84, 0.91)
//   Tier 4 Developing  → D ∈ [0.72, 0.84)
//   Tier 5 Struggling  → D ∈ [0.40, 0.72)
fun tierOf(bot: BotProfile): String = when {
    bot.baseDiscipline >= 0.96 -> "1 — Elite"
    bot.baseDiscipline >= 0.91 -> "2 — Advanced"
    bot.baseDiscipline >= 0.84 -> "3 — Intermediate"
    bot.baseDiscipline >= 0.72 -> "4 — Developing"
    else                       -> "5 — Struggling"
}

// ── Bot generation ────────────────────────────────────────────────────────────
fun generateBots(): List<BotProfile> {
    val bots = mutableListOf<BotProfile>()
    var globalId = 0

    WarriorRegion.entries.forEachIndexed { rIdx, region ->
        val names = namesForRegion(region).shuffled(Random(rIdx.toLong() * 7919L))
        repeat(150) { i ->
            val (dBase, fFactor) = when {
                i < 10  -> Pair(Random.nextDouble(0.96, 0.99), Random.nextDouble(0.003, 0.005))   // Tier 1 Elite
                i < 30  -> Pair(Random.nextDouble(0.91, 0.96), Random.nextDouble(0.005, 0.008))   // Tier 2
                i < 60  -> Pair(Random.nextDouble(0.84, 0.91), Random.nextDouble(0.007, 0.011))   // Tier 3
                i < 100 -> Pair(Random.nextDouble(0.72, 0.84), Random.nextDouble(0.009, 0.014))   // Tier 4
                else    -> Pair(Random.nextDouble(0.40, 0.72), Random.nextDouble(0.012, 0.020))   // Tier 5 Struggling
            }
            bots.add(
                BotProfile(
                    id             = globalId++,
                    name           = names.getOrElse(i) { "Warrior${globalId}" },
                    region         = region.name,
                    baseDiscipline = dBase,
                    fatigueFactor  = fFactor,
                    seed           = (rIdx * 150L + i) * 31337L
                )
            )
        }
    }
    return bots
}

// ── Procedural calendar generation (never stored, generated on demand) ────────
fun generateBotCalendar(bot: BotProfile, days: Int = 365): Map<String, Boolean> {
    val rng = Random(bot.seed)
    val result = LinkedHashMap<String, Boolean>()
    val startDate = java.time.LocalDate.now().minusDays(days.toLong() - 1)
    var tempStreak = 0
    var tempMomentum = 0.0

    repeat(days) { dayIdx ->
        val date    = startDate.plusDays(dayIdx.toLong())
        val key     = date.format(DATE_FORMATTER)
        val tempBot = bot.copy(currentStreak = tempStreak, momentum = tempMomentum)
        val prob    = survivalProbability(tempBot)
        val clean   = rng.nextDouble() < prob
        result[key] = clean
        if (clean) {
            tempStreak++
            tempMomentum = min(tempMomentum + 1.0, 30.0)
        } else {
            tempStreak = 0
            tempMomentum = max(tempMomentum - 3.0, 0.0)
        }
    }
    return result
}
