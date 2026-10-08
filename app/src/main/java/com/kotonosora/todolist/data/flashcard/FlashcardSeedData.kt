package com.kotonosora.todolist.data.flashcard

/** Built-in deck seed content. Single source of truth for seeded decks.
 * Lives in data so the repository can seed without depending on feature;
 * `DemoFlashcardData` (feature) wraps it for demo playback. */
object FlashcardSeedData {
    data class SeedCard(
        val word: String,
        val definition: String = "",
        val phonetic: String = "",
        val example: String = ""
    )

    data class SeedDeck(val id: String, val name: String, val description: String)

    val seedDecks = listOf(
        SeedDeck(
            "demo_basic",
            "Basic Vocabulary",
            "Common everyday words like student, school, afternoon."
        ),
        SeedDeck(
            "demo_advanced",
            "Advanced Vocabulary",
            "Complex words to expand your vocabulary."
        ),
        SeedDeck(
            "demo_tech",
            "Tech Terminology",
            "Words used in software engineering and technology."
        )
    )

    fun demoCards(deckId: String): Pair<String, List<SeedCard>> {
        return when (deckId) {
            "demo_basic" -> "Basic Vocabulary" to listOf(
                SeedCard(
                    word = "Student",
                    definition = "A person who is studying at a school or college.",
                    phonetic = "/ˈstjuː.dənt/",
                    example = "She is a top student in her class."
                ),
                SeedCard(
                    word = "School",
                    definition = "An institution for educating children or adults.",
                    phonetic = "/skuːl/",
                    example = "They walk to school every morning."
                ),
                SeedCard(
                    word = "Afternoon",
                    definition = "The time from noon or lunchtime to evening.",
                    phonetic = "/ˌɑːf.təˈnuːn/",
                    example = "We had tea in the afternoon."
                ),
                SeedCard(
                    word = "Teacher",
                    definition = "A person who teaches, especially in a school.",
                    phonetic = "/ˈtiː.tʃər/",
                    example = "The teacher explained the lesson clearly."
                ),
                SeedCard(
                    word = "Library",
                    definition = "A building containing collections of books for reading or borrowing.",
                    phonetic = "/ˈlaɪ.brər.i/",
                    example = "Quiet studying is required in the library."
                ),
                SeedCard(
                    word = "Book",
                    definition = "A set of printed pages held together in a cover.",
                    phonetic = "/bʊk/",
                    example = "She borrowed a book from the library."
                ),
                SeedCard(
                    word = "Water",
                    definition = "The clear liquid that falls as rain and fills rivers.",
                    phonetic = "/ˈwɔː.tər/",
                    example = "Drink more water every day."
                ),
                SeedCard(
                    word = "House",
                    definition = "A building where people live.",
                    phonetic = "/haʊs/",
                    example = "They bought a new house downtown."
                ),
                SeedCard(
                    word = "Family",
                    definition = "A group of people related by blood or marriage.",
                    phonetic = "/ˈfæm.əl.i/",
                    example = "Her family lives nearby."
                ),
                SeedCard(
                    word = "Friend",
                    definition = "A person you know well and like.",
                    phonetic = "/frend/",
                    example = "He is my best friend."
                ),
                SeedCard(
                    word = "Morning",
                    definition = "The early part of the day, before noon.",
                    phonetic = "/ˈmɔː.nɪŋ/",
                    example = "Good morning, class!"
                ),
                SeedCard(
                    word = "Evening",
                    definition = "The end of the day, before night.",
                    phonetic = "/ˈiːv.nɪŋ/",
                    example = "We walk in the evening."
                ),
                SeedCard(
                    word = "Food",
                    definition = "Things that people eat.",
                    phonetic = "/fuːd/",
                    example = "This food is delicious."
                ),
                SeedCard(
                    word = "Dog",
                    definition = "A common pet animal that barks.",
                    phonetic = "/dɒɡ/",
                    example = "The dog barks loudly."
                ),
                SeedCard(
                    word = "Cat",
                    definition = "A small pet animal that meows.",
                    phonetic = "/kæt/",
                    example = "The cat sleeps all day."
                ),
                SeedCard(
                    word = "Sun",
                    definition = "The star that gives light and heat to Earth.",
                    phonetic = "/sʌn/",
                    example = "The sun rises in the east."
                ),
                SeedCard(
                    word = "Moon",
                    definition = "The round object that shines in the night sky.",
                    phonetic = "/muːn/",
                    example = "The moon is bright tonight."
                ),
                SeedCard(
                    word = "Tree",
                    definition = "A tall plant with a wooden trunk and branches.",
                    phonetic = "/triː/",
                    example = "Birds sit in the tree."
                ),
                SeedCard(
                    word = "Car",
                    definition = "A vehicle with four wheels used for travel.",
                    phonetic = "/kɑːr/",
                    example = "He drives a red car."
                ),
                SeedCard(
                    word = "Bus",
                    definition = "A large vehicle that carries many passengers.",
                    phonetic = "/bʌs/",
                    example = "We take the bus to work."
                ),
                SeedCard(
                    word = "Happy",
                    definition = "Feeling pleased and joyful.",
                    phonetic = "/ˈhæp.i/",
                    example = "She feels happy today."
                ),
                SeedCard(
                    word = "Sad",
                    definition = "Feeling unhappy or sorrowful.",
                    phonetic = "/sæd/",
                    example = "He was sad to leave."
                ),
                SeedCard(
                    word = "Big",
                    definition = "Large in size.",
                    phonetic = "/bɪɡ/",
                    example = "An elephant is big."
                ),
                SeedCard(
                    word = "Small",
                    definition = "Little in size.",
                    phonetic = "/smɔːl/",
                    example = "A mouse is small."
                ),
                SeedCard(
                    word = "Fast",
                    definition = "Moving quickly.",
                    phonetic = "/fɑːst/",
                    example = "Cheetahs run fast."
                ),
                SeedCard(
                    word = "Slow",
                    definition = "Moving at a low speed.",
                    phonetic = "/sləʊ/",
                    example = "Turtles walk slow."
                ),
                SeedCard(
                    word = "Hot",
                    definition = "Having a high temperature.",
                    phonetic = "/hɒt/",
                    example = "The soup is hot."
                ),
                SeedCard(
                    word = "Cold",
                    definition = "Having a low temperature.",
                    phonetic = "/kəʊld/",
                    example = "Winter is cold."
                ),
                SeedCard(
                    word = "Day",
                    definition = "The time between sunrise and sunset.",
                    phonetic = "/deɪ/",
                    example = "Have a nice day!"
                ),
                SeedCard(
                    word = "Night",
                    definition = "The dark time between sunset and sunrise.",
                    phonetic = "/naɪt/",
                    example = "Good night, sleep well."
                )
            )

            "demo_advanced" -> "Advanced Vocabulary" to listOf(
                SeedCard(
                    word = "Serendipity",
                    definition = "The occurrence of events by chance in a happy or beneficial way.",
                    phonetic = "/ˌser.ənˈdɪp.ə.ti/",
                    example = "Finding the lost key was pure serendipity."
                ),
                SeedCard(
                    word = "Ephemeral",
                    definition = "Lasting for a very short time; fleeting.",
                    phonetic = "/ɪˈfem.ər.əl/",
                    example = "Fame in the digital age can be ephemeral."
                ),
                SeedCard(
                    word = "Ubiquitous",
                    definition = "Present, appearing, or found everywhere.",
                    phonetic = "/juːˈbɪk.wɪ.təs/",
                    example = "Smartphones have become ubiquitous in daily life."
                ),
                SeedCard(
                    word = "Mellifluous",
                    definition = "Sweet or musical; pleasant to hear.",
                    phonetic = "/məˈlɪf.lu.əs/",
                    example = "Her mellifluous voice relaxed the audience."
                ),
                SeedCard(
                    word = "Ineffable",
                    definition = "Too great or extreme to be expressed or described in words.",
                    phonetic = "/ɪnˈef.ə.bəl/",
                    example = "The beauty of the sunset was ineffable."
                ),
                SeedCard(
                    word = "Petrichor",
                    definition = "The pleasant smell after rain falls on dry soil.",
                    phonetic = "/ˈpet.rɪ.kɔːr/",
                    example = "Petrichor filled the air after the storm."
                ),
                SeedCard(
                    word = "Sonorous",
                    definition = "Producing a deep, full, reverberating sound.",
                    phonetic = "/ˈsɒn.ər.əs/",
                    example = "His sonorous voice filled the hall."
                ),
                SeedCard(
                    word = "Lethargic",
                    definition = "Lacking energy; feeling sluggish.",
                    phonetic = "/ləˈθɑː.dʒɪk/",
                    example = "Hot weather makes me lethargic."
                ),
                SeedCard(
                    word = "Pragmatic",
                    definition = "Guided by practical experience rather than theory.",
                    phonetic = "/præɡˈmæt.ɪk/",
                    example = "Be pragmatic about deadlines."
                ),
                SeedCard(
                    word = "Voracious",
                    definition = "Eager for knowledge or food; insatiable.",
                    phonetic = "/vəˈreɪ.ʃəs/",
                    example = "She is a voracious reader."
                ),
                SeedCard(
                    word = "Enigmatic",
                    definition = "Mysterious and difficult to understand.",
                    phonetic = "/ˌen.ɪɡˈmæt.ɪk/",
                    example = "His enigmatic smile puzzled us."
                ),
                SeedCard(
                    word = "Resilient",
                    definition = "Able to recover quickly from difficulties.",
                    phonetic = "/rɪˈzɪl.jənt/",
                    example = "Children are remarkably resilient."
                ),
                SeedCard(
                    word = "Eloquent",
                    definition = "Fluent and expressive in speech.",
                    phonetic = "/ˈel.ə.kwənt/",
                    example = "Her eloquent speech moved the voters."
                ),
                SeedCard(
                    word = "Meticulous",
                    definition = "Showing great attention to detail.",
                    phonetic = "/məˈtɪk.jə.ləs/",
                    example = "Meticulous planning prevents errors."
                ),
                SeedCard(
                    word = "Ambiguous",
                    definition = "Open to more than one interpretation.",
                    phonetic = "/æmˈbɪɡ.ju.əs/",
                    example = "His answer was ambiguous."
                ),
                SeedCard(
                    word = "Candid",
                    definition = "Honest and straightforward.",
                    phonetic = "/ˈkæn.dɪd/",
                    example = "She gave a candid interview."
                ),
                SeedCard(
                    word = "Diligent",
                    definition = "Showing steady, earnest effort.",
                    phonetic = "/ˈdɪl.ɪ.dʒənt/",
                    example = "Diligent students pass easily."
                ),
                SeedCard(
                    word = "Empathy",
                    definition = "The ability to understand others' feelings.",
                    phonetic = "/ˈem.pə.θi/",
                    example = "Nurses need great empathy."
                ),
                SeedCard(
                    word = "Frugal",
                    definition = "Careful with money; economical.",
                    phonetic = "/ˈfruː.ɡəl/",
                    example = "Frugal habits build savings."
                ),
                SeedCard(
                    word = "Gregarious",
                    definition = "Fond of company; sociable.",
                    phonetic = "/ɡreˈɡeə.ri.əs/",
                    example = "Gregarious people love parties."
                ),
                SeedCard(
                    word = "Humble",
                    definition = "Modest; not arrogant.",
                    phonetic = "/ˈhʌm.bəl/",
                    example = "Stay humble after success."
                ),
                SeedCard(
                    word = "Inevitable",
                    definition = "Certain to happen; unavoidable.",
                    phonetic = "/ɪnˈev.ɪ.tə.bəl/",
                    example = "Change is inevitable."
                ),
                SeedCard(
                    word = "Jubilant",
                    definition = "Feeling great joy and triumph.",
                    phonetic = "/ˈdʒuː.bɪ.lənt/",
                    example = "Fans were jubilant after the victory."
                ),
                SeedCard(
                    word = "Keen",
                    definition = "Eager and enthusiastic.",
                    phonetic = "/kiːn/",
                    example = "She is keen to learn piano."
                ),
                SeedCard(
                    word = "Lucid",
                    definition = "Clear and easy to understand.",
                    phonetic = "/ˈluː.sɪd/",
                    example = "Write lucid instructions."
                ),
                SeedCard(
                    word = "Nostalgia",
                    definition = "Longing for the past.",
                    phonetic = "/nɒˈstæl.dʒə/",
                    example = "Old songs bring nostalgia."
                ),
                SeedCard(
                    word = "Obstinate",
                    definition = "Stubbornly refusing to change.",
                    phonetic = "/ˈɒb.stɪ.nət/",
                    example = "The obstinate child refused help."
                ),
                SeedCard(
                    word = "Placate",
                    definition = "To calm someone's anger.",
                    phonetic = "/pləˈkeɪt/",
                    example = "Apologize to placate customers."
                ),
                SeedCard(
                    word = "Quaint",
                    definition = "Charmingly old-fashioned.",
                    phonetic = "/kweɪnt/",
                    example = "A quaint village by the lake."
                ),
                SeedCard(
                    word = "Tenacious",
                    definition = "Holding firmly; persistent.",
                    phonetic = "/tɪˈneɪ.ʃəs/",
                    example = "Tenacious effort wins marathons."
                )
            )

            "demo_tech" -> "Tech Terminology" to listOf(
                SeedCard(
                    word = "Algorithm",
                    definition = "A process or set of rules to be followed in calculations or problem-solving.",
                    phonetic = "/ˈæl.ɡə.rɪ.ðəm/",
                    example = "Sorting algorithms optimize search times."
                ),
                SeedCard(
                    word = "Database",
                    definition = "An organized collection of structured information or data stored electronically.",
                    phonetic = "/ˈdeɪ.tə.beɪs/",
                    example = "Room SQLite database manages offline app state."
                ),
                SeedCard(
                    word = "Encryption",
                    definition = "The process of converting information or data into code to prevent unauthorized access.",
                    phonetic = "/ɪnˈkrɪp.ʃən/",
                    example = "End-to-end encryption keeps messages private."
                ),
                SeedCard(
                    word = "Polymorphism",
                    definition = "The condition of occurring in several different forms in OOP.",
                    phonetic = "/ˌpɒl.iˈmɔː.fɪ.zəm/",
                    example = "Method overriding demonstrates runtime polymorphism."
                ),
                SeedCard(
                    word = "Recursion",
                    definition = "A method where the solution depends on solutions to smaller instances of the same problem.",
                    phonetic = "/rɪˈkɜː.ʃən/",
                    example = "Factorial logic is cleanly solved using recursion."
                ),
                SeedCard(
                    word = "Compiler",
                    definition = "A program that translates code into machine language.",
                    phonetic = "/kəmˈpaɪ.lər/",
                    example = "The Kotlin compiler checks types."
                ),
                SeedCard(
                    word = "Framework",
                    definition = "A reusable platform for building software.",
                    phonetic = "/ˈfreɪm.wɜːrk/",
                    example = "Compose is a UI framework."
                ),
                SeedCard(
                    word = "Repository",
                    definition = "A storage location for code and files.",
                    phonetic = "/rɪˈpɒz.ɪ.tər.i/",
                    example = "Push commits to the repository."
                ),
                SeedCard(
                    word = "Interface",
                    definition = "A shared boundary for interaction between components.",
                    phonetic = "/ˈɪn.tə.feɪs/",
                    example = "Clean interfaces reduce coupling."
                ),
                SeedCard(
                    word = "Bandwidth",
                    definition = "The capacity for data transfer over a network.",
                    phonetic = "/ˈbænd.wɪdθ/",
                    example = "Video calls need high bandwidth."
                ),
                SeedCard(
                    word = "Cache",
                    definition = "Fast temporary storage for quick access.",
                    phonetic = "/kæʃ/",
                    example = "Clear the cache to free space."
                ),
                SeedCard(
                    word = "Debugging",
                    definition = "The process of finding and fixing errors.",
                    phonetic = "/ˌdiːˈbʌɡ.ɪŋ/",
                    example = "Debugging took all morning."
                ),
                SeedCard(
                    word = "Firewall",
                    definition = "A network security barrier.",
                    phonetic = "/ˈfaɪə.wɔːl/",
                    example = "The firewall blocks attacks."
                ),
                SeedCard(
                    word = "Gigabyte",
                    definition = "A unit of digital storage.",
                    phonetic = "/ˈɡɪɡ.ə.baɪt/",
                    example = "The video uses two gigabytes."
                ),
                SeedCard(
                    word = "Hacker",
                    definition = "A skilled programmer, or an intruder.",
                    phonetic = "/ˈhæk.ər/",
                    example = "The hacker patched the bug."
                ),
                SeedCard(
                    word = "Iteration",
                    definition = "A repetition cycle that improves a result.",
                    phonetic = "/ˌɪt.əˈreɪ.ʃən/",
                    example = "Each iteration improves the app."
                ),
                SeedCard(
                    word = "Kernel",
                    definition = "The core of an operating system.",
                    phonetic = "/ˈkɜː.nəl/",
                    example = "The kernel manages memory."
                ),
                SeedCard(
                    word = "Latency",
                    definition = "Delay in data transfer.",
                    phonetic = "/ˈleɪ.tən.si/",
                    example = "Low latency matters for gaming."
                ),
                SeedCard(
                    word = "Malware",
                    definition = "Malicious software.",
                    phonetic = "/ˈmæl.weər/",
                    example = "Antivirus removes malware."
                ),
                SeedCard(
                    word = "Node",
                    definition = "A connection point in a network.",
                    phonetic = "/nəʊd/",
                    example = "Each server is a node."
                ),
                SeedCard(
                    word = "Pixel",
                    definition = "The smallest unit of a display.",
                    phonetic = "/ˈpɪk.səl/",
                    example = "More pixels mean sharper text."
                ),
                SeedCard(
                    word = "Query",
                    definition = "A request for data.",
                    phonetic = "/ˈkwɪə.ri/",
                    example = "The query returns ten rows."
                ),
                SeedCard(
                    word = "Server",
                    definition = "A computer that serves client requests.",
                    phonetic = "/ˈsɜː.vər/",
                    example = "Restart the server tonight."
                ),
                SeedCard(
                    word = "Token",
                    definition = "A unit of authentication or text.",
                    phonetic = "/ˈtəʊ.kən/",
                    example = "Paste your API token here."
                ),
                SeedCard(
                    word = "Uptime",
                    definition = "The time a system stays running.",
                    phonetic = "/ˈʌp.taɪm/",
                    example = "99.9% uptime is the goal."
                ),
                SeedCard(
                    word = "Version",
                    definition = "A specific release of software.",
                    phonetic = "/ˈvɜː.ʃən/",
                    example = "Update to the latest version."
                ),
                SeedCard(
                    word = "Widget",
                    definition = "A small UI component.",
                    phonetic = "/ˈwɪdʒ.ɪt/",
                    example = "Add a search widget at home."
                ),
                SeedCard(
                    word = "Syntax",
                    definition = "The rules of code structure.",
                    phonetic = "/ˈsɪn.tæks/",
                    example = "Check the syntax carefully."
                ),
                SeedCard(
                    word = "Boolean",
                    definition = "A true/false value type.",
                    phonetic = "/ˈbuː.li.ən/",
                    example = "Return a boolean flag."
                ),
                SeedCard(
                    word = "Frontend",
                    definition = "The user-facing part of software.",
                    phonetic = "/ˈfrʌnt.end/",
                    example = "She works on frontend design."
                )
            )

            else -> "Sample Flashcards" to listOf(
                SeedCard(
                    word = "Apple",
                    definition = "A round red or green fruit with firm white flesh.",
                    phonetic = "/ˈæp.əl/"
                ),
                SeedCard(
                    word = "Banana",
                    definition = "A long curved fruit with a yellow skin.",
                    phonetic = "/bəˈnɑː.nə/"
                ),
                SeedCard(
                    word = "Cherry",
                    definition = "A small, round, bright or dark red fruit.",
                    phonetic = "/ˈtʃer.i/"
                )
            )
        }
    }
}
