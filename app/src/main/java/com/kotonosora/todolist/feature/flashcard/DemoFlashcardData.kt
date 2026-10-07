package com.kotonosora.todolist.feature.flashcard

/** Built-in deck seed data. Single source of truth for demo decks. */
object DemoFlashcardData {
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

    fun demoCards(deckId: String): Pair<String, List<Flashcard>> {
        return when (deckId) {
            "demo_basic" -> "Basic Vocabulary" to listOf(
                Flashcard(
                    word = "Student",
                    definition = "A person who is studying at a school or college.",
                    phonetic = "/ˈstjuː.dənt/",
                    example = "She is a top student in her class."
                ),
                Flashcard(
                    word = "School",
                    definition = "An institution for educating children or adults.",
                    phonetic = "/skuːl/",
                    example = "They walk to school every morning."
                ),
                Flashcard(
                    word = "Afternoon",
                    definition = "The time from noon or lunchtime to evening.",
                    phonetic = "/ˌɑːf.təˈnuːn/",
                    example = "We had tea in the afternoon."
                ),
                Flashcard(
                    word = "Teacher",
                    definition = "A person who teaches, especially in a school.",
                    phonetic = "/ˈtiː.tʃər/",
                    example = "The teacher explained the lesson clearly."
                ),
                Flashcard(
                    word = "Library",
                    definition = "A building containing collections of books for reading or borrowing.",
                    phonetic = "/ˈlaɪ.brər.i/",
                    example = "Quiet studying is required in the library."
                ),
                Flashcard(
                    word = "Book",
                    definition = "A set of printed pages held together in a cover.",
                    phonetic = "/bʊk/",
                    example = "She borrowed a book from the library."
                ),
                Flashcard(
                    word = "Water",
                    definition = "The clear liquid that falls as rain and fills rivers.",
                    phonetic = "/ˈwɔː.tər/",
                    example = "Drink more water every day."
                ),
                Flashcard(
                    word = "House",
                    definition = "A building where people live.",
                    phonetic = "/haʊs/",
                    example = "They bought a new house downtown."
                ),
                Flashcard(
                    word = "Family",
                    definition = "A group of people related by blood or marriage.",
                    phonetic = "/ˈfæm.əl.i/",
                    example = "Her family lives nearby."
                ),
                Flashcard(
                    word = "Friend",
                    definition = "A person you know well and like.",
                    phonetic = "/frend/",
                    example = "He is my best friend."
                ),
                Flashcard(
                    word = "Morning",
                    definition = "The early part of the day, before noon.",
                    phonetic = "/ˈmɔː.nɪŋ/",
                    example = "Good morning, class!"
                ),
                Flashcard(
                    word = "Evening",
                    definition = "The end of the day, before night.",
                    phonetic = "/ˈiːv.nɪŋ/",
                    example = "We walk in the evening."
                ),
                Flashcard(
                    word = "Food",
                    definition = "Things that people eat.",
                    phonetic = "/fuːd/",
                    example = "This food is delicious."
                ),
                Flashcard(
                    word = "Dog",
                    definition = "A common pet animal that barks.",
                    phonetic = "/dɒɡ/",
                    example = "The dog barks loudly."
                ),
                Flashcard(
                    word = "Cat",
                    definition = "A small pet animal that meows.",
                    phonetic = "/kæt/",
                    example = "The cat sleeps all day."
                ),
                Flashcard(
                    word = "Sun",
                    definition = "The star that gives light and heat to Earth.",
                    phonetic = "/sʌn/",
                    example = "The sun rises in the east."
                ),
                Flashcard(
                    word = "Moon",
                    definition = "The round object that shines in the night sky.",
                    phonetic = "/muːn/",
                    example = "The moon is bright tonight."
                ),
                Flashcard(
                    word = "Tree",
                    definition = "A tall plant with a wooden trunk and branches.",
                    phonetic = "/triː/",
                    example = "Birds sit in the tree."
                ),
                Flashcard(
                    word = "Car",
                    definition = "A vehicle with four wheels used for travel.",
                    phonetic = "/kɑːr/",
                    example = "He drives a red car."
                ),
                Flashcard(
                    word = "Bus",
                    definition = "A large vehicle that carries many passengers.",
                    phonetic = "/bʌs/",
                    example = "We take the bus to work."
                ),
                Flashcard(
                    word = "Happy",
                    definition = "Feeling pleased and joyful.",
                    phonetic = "/ˈhæp.i/",
                    example = "She feels happy today."
                ),
                Flashcard(
                    word = "Sad",
                    definition = "Feeling unhappy or sorrowful.",
                    phonetic = "/sæd/",
                    example = "He was sad to leave."
                ),
                Flashcard(
                    word = "Big",
                    definition = "Large in size.",
                    phonetic = "/bɪɡ/",
                    example = "An elephant is big."
                ),
                Flashcard(
                    word = "Small",
                    definition = "Little in size.",
                    phonetic = "/smɔːl/",
                    example = "A mouse is small."
                ),
                Flashcard(
                    word = "Fast",
                    definition = "Moving quickly.",
                    phonetic = "/fɑːst/",
                    example = "Cheetahs run fast."
                ),
                Flashcard(
                    word = "Slow",
                    definition = "Moving at a low speed.",
                    phonetic = "/sləʊ/",
                    example = "Turtles walk slow."
                ),
                Flashcard(
                    word = "Hot",
                    definition = "Having a high temperature.",
                    phonetic = "/hɒt/",
                    example = "The soup is hot."
                ),
                Flashcard(
                    word = "Cold",
                    definition = "Having a low temperature.",
                    phonetic = "/kəʊld/",
                    example = "Winter is cold."
                ),
                Flashcard(
                    word = "Day",
                    definition = "The time between sunrise and sunset.",
                    phonetic = "/deɪ/",
                    example = "Have a nice day!"
                ),
                Flashcard(
                    word = "Night",
                    definition = "The dark time between sunset and sunrise.",
                    phonetic = "/naɪt/",
                    example = "Good night, sleep well."
                )
            )

            "demo_advanced" -> "Advanced Vocabulary" to listOf(
                Flashcard(
                    word = "Serendipity",
                    definition = "The occurrence of events by chance in a happy or beneficial way.",
                    phonetic = "/ˌser.ənˈdɪp.ə.ti/",
                    example = "Finding the lost key was pure serendipity."
                ),
                Flashcard(
                    word = "Ephemeral",
                    definition = "Lasting for a very short time; fleeting.",
                    phonetic = "/ɪˈfem.ər.əl/",
                    example = "Fame in the digital age can be ephemeral."
                ),
                Flashcard(
                    word = "Ubiquitous",
                    definition = "Present, appearing, or found everywhere.",
                    phonetic = "/juːˈbɪk.wɪ.təs/",
                    example = "Smartphones have become ubiquitous in daily life."
                ),
                Flashcard(
                    word = "Mellifluous",
                    definition = "Sweet or musical; pleasant to hear.",
                    phonetic = "/məˈlɪf.lu.əs/",
                    example = "Her mellifluous voice relaxed the audience."
                ),
                Flashcard(
                    word = "Ineffable",
                    definition = "Too great or extreme to be expressed or described in words.",
                    phonetic = "/ɪnˈef.ə.bəl/",
                    example = "The beauty of the sunset was ineffable."
                ),
                Flashcard(
                    word = "Petrichor",
                    definition = "The pleasant smell after rain falls on dry soil.",
                    phonetic = "/ˈpet.rɪ.kɔːr/",
                    example = "Petrichor filled the air after the storm."
                ),
                Flashcard(
                    word = "Sonorous",
                    definition = "Producing a deep, full, reverberating sound.",
                    phonetic = "/ˈsɒn.ər.əs/",
                    example = "His sonorous voice filled the hall."
                ),
                Flashcard(
                    word = "Lethargic",
                    definition = "Lacking energy; feeling sluggish.",
                    phonetic = "/ləˈθɑː.dʒɪk/",
                    example = "Hot weather makes me lethargic."
                ),
                Flashcard(
                    word = "Pragmatic",
                    definition = "Guided by practical experience rather than theory.",
                    phonetic = "/præɡˈmæt.ɪk/",
                    example = "Be pragmatic about deadlines."
                ),
                Flashcard(
                    word = "Voracious",
                    definition = "Eager for knowledge or food; insatiable.",
                    phonetic = "/vəˈreɪ.ʃəs/",
                    example = "She is a voracious reader."
                ),
                Flashcard(
                    word = "Enigmatic",
                    definition = "Mysterious and difficult to understand.",
                    phonetic = "/ˌen.ɪɡˈmæt.ɪk/",
                    example = "His enigmatic smile puzzled us."
                ),
                Flashcard(
                    word = "Resilient",
                    definition = "Able to recover quickly from difficulties.",
                    phonetic = "/rɪˈzɪl.jənt/",
                    example = "Children are remarkably resilient."
                ),
                Flashcard(
                    word = "Eloquent",
                    definition = "Fluent and expressive in speech.",
                    phonetic = "/ˈel.ə.kwənt/",
                    example = "Her eloquent speech moved the voters."
                ),
                Flashcard(
                    word = "Meticulous",
                    definition = "Showing great attention to detail.",
                    phonetic = "/məˈtɪk.jə.ləs/",
                    example = "Meticulous planning prevents errors."
                ),
                Flashcard(
                    word = "Ambiguous",
                    definition = "Open to more than one interpretation.",
                    phonetic = "/æmˈbɪɡ.ju.əs/",
                    example = "His answer was ambiguous."
                ),
                Flashcard(
                    word = "Candid",
                    definition = "Honest and straightforward.",
                    phonetic = "/ˈkæn.dɪd/",
                    example = "She gave a candid interview."
                ),
                Flashcard(
                    word = "Diligent",
                    definition = "Showing steady, earnest effort.",
                    phonetic = "/ˈdɪl.ɪ.dʒənt/",
                    example = "Diligent students pass easily."
                ),
                Flashcard(
                    word = "Empathy",
                    definition = "The ability to understand others' feelings.",
                    phonetic = "/ˈem.pə.θi/",
                    example = "Nurses need great empathy."
                ),
                Flashcard(
                    word = "Frugal",
                    definition = "Careful with money; economical.",
                    phonetic = "/ˈfruː.ɡəl/",
                    example = "Frugal habits build savings."
                ),
                Flashcard(
                    word = "Gregarious",
                    definition = "Fond of company; sociable.",
                    phonetic = "/ɡreˈɡeə.ri.əs/",
                    example = "Gregarious people love parties."
                ),
                Flashcard(
                    word = "Humble",
                    definition = "Modest; not arrogant.",
                    phonetic = "/ˈhʌm.bəl/",
                    example = "Stay humble after success."
                ),
                Flashcard(
                    word = "Inevitable",
                    definition = "Certain to happen; unavoidable.",
                    phonetic = "/ɪnˈev.ɪ.tə.bəl/",
                    example = "Change is inevitable."
                ),
                Flashcard(
                    word = "Jubilant",
                    definition = "Feeling great joy and triumph.",
                    phonetic = "/ˈdʒuː.bɪ.lənt/",
                    example = "Fans were jubilant after the victory."
                ),
                Flashcard(
                    word = "Keen",
                    definition = "Eager and enthusiastic.",
                    phonetic = "/kiːn/",
                    example = "She is keen to learn piano."
                ),
                Flashcard(
                    word = "Lucid",
                    definition = "Clear and easy to understand.",
                    phonetic = "/ˈluː.sɪd/",
                    example = "Write lucid instructions."
                ),
                Flashcard(
                    word = "Nostalgia",
                    definition = "Longing for the past.",
                    phonetic = "/nɒˈstæl.dʒə/",
                    example = "Old songs bring nostalgia."
                ),
                Flashcard(
                    word = "Obstinate",
                    definition = "Stubbornly refusing to change.",
                    phonetic = "/ˈɒb.stɪ.nət/",
                    example = "The obstinate child refused help."
                ),
                Flashcard(
                    word = "Placate",
                    definition = "To calm someone's anger.",
                    phonetic = "/pləˈkeɪt/",
                    example = "Apologize to placate customers."
                ),
                Flashcard(
                    word = "Quaint",
                    definition = "Charmingly old-fashioned.",
                    phonetic = "/kweɪnt/",
                    example = "A quaint village by the lake."
                ),
                Flashcard(
                    word = "Tenacious",
                    definition = "Holding firmly; persistent.",
                    phonetic = "/tɪˈneɪ.ʃəs/",
                    example = "Tenacious effort wins marathons."
                )
            )

            "demo_tech" -> "Tech Terminology" to listOf(
                Flashcard(
                    word = "Algorithm",
                    definition = "A process or set of rules to be followed in calculations or problem-solving.",
                    phonetic = "/ˈæl.ɡə.rɪ.ðəm/",
                    example = "Sorting algorithms optimize search times."
                ),
                Flashcard(
                    word = "Database",
                    definition = "An organized collection of structured information or data stored electronically.",
                    phonetic = "/ˈdeɪ.tə.beɪs/",
                    example = "Room SQLite database manages offline app state."
                ),
                Flashcard(
                    word = "Encryption",
                    definition = "The process of converting information or data into code to prevent unauthorized access.",
                    phonetic = "/ɪnˈkrɪp.ʃən/",
                    example = "End-to-end encryption keeps messages private."
                ),
                Flashcard(
                    word = "Polymorphism",
                    definition = "The condition of occurring in several different forms in OOP.",
                    phonetic = "/ˌpɒl.iˈmɔː.fɪ.zəm/",
                    example = "Method overriding demonstrates runtime polymorphism."
                ),
                Flashcard(
                    word = "Recursion",
                    definition = "A method where the solution depends on solutions to smaller instances of the same problem.",
                    phonetic = "/rɪˈkɜː.ʃən/",
                    example = "Factorial logic is cleanly solved using recursion."
                ),
                Flashcard(
                    word = "Compiler",
                    definition = "A program that translates code into machine language.",
                    phonetic = "/kəmˈpaɪ.lər/",
                    example = "The Kotlin compiler checks types."
                ),
                Flashcard(
                    word = "Framework",
                    definition = "A reusable platform for building software.",
                    phonetic = "/ˈfreɪm.wɜːrk/",
                    example = "Compose is a UI framework."
                ),
                Flashcard(
                    word = "Repository",
                    definition = "A storage location for code and files.",
                    phonetic = "/rɪˈpɒz.ɪ.tər.i/",
                    example = "Push commits to the repository."
                ),
                Flashcard(
                    word = "Interface",
                    definition = "A shared boundary for interaction between components.",
                    phonetic = "/ˈɪn.tə.feɪs/",
                    example = "Clean interfaces reduce coupling."
                ),
                Flashcard(
                    word = "Bandwidth",
                    definition = "The capacity for data transfer over a network.",
                    phonetic = "/ˈbænd.wɪdθ/",
                    example = "Video calls need high bandwidth."
                ),
                Flashcard(
                    word = "Cache",
                    definition = "Fast temporary storage for quick access.",
                    phonetic = "/kæʃ/",
                    example = "Clear the cache to free space."
                ),
                Flashcard(
                    word = "Debugging",
                    definition = "The process of finding and fixing errors.",
                    phonetic = "/ˌdiːˈbʌɡ.ɪŋ/",
                    example = "Debugging took all morning."
                ),
                Flashcard(
                    word = "Firewall",
                    definition = "A network security barrier.",
                    phonetic = "/ˈfaɪə.wɔːl/",
                    example = "The firewall blocks attacks."
                ),
                Flashcard(
                    word = "Gigabyte",
                    definition = "A unit of digital storage.",
                    phonetic = "/ˈɡɪɡ.ə.baɪt/",
                    example = "The video uses two gigabytes."
                ),
                Flashcard(
                    word = "Hacker",
                    definition = "A skilled programmer, or an intruder.",
                    phonetic = "/ˈhæk.ər/",
                    example = "The hacker patched the bug."
                ),
                Flashcard(
                    word = "Iteration",
                    definition = "A repetition cycle that improves a result.",
                    phonetic = "/ˌɪt.əˈreɪ.ʃən/",
                    example = "Each iteration improves the app."
                ),
                Flashcard(
                    word = "Kernel",
                    definition = "The core of an operating system.",
                    phonetic = "/ˈkɜː.nəl/",
                    example = "The kernel manages memory."
                ),
                Flashcard(
                    word = "Latency",
                    definition = "Delay in data transfer.",
                    phonetic = "/ˈleɪ.tən.si/",
                    example = "Low latency matters for gaming."
                ),
                Flashcard(
                    word = "Malware",
                    definition = "Malicious software.",
                    phonetic = "/ˈmæl.weər/",
                    example = "Antivirus removes malware."
                ),
                Flashcard(
                    word = "Node",
                    definition = "A connection point in a network.",
                    phonetic = "/nəʊd/",
                    example = "Each server is a node."
                ),
                Flashcard(
                    word = "Pixel",
                    definition = "The smallest unit of a display.",
                    phonetic = "/ˈpɪk.səl/",
                    example = "More pixels mean sharper text."
                ),
                Flashcard(
                    word = "Query",
                    definition = "A request for data.",
                    phonetic = "/ˈkwɪə.ri/",
                    example = "The query returns ten rows."
                ),
                Flashcard(
                    word = "Server",
                    definition = "A computer that serves client requests.",
                    phonetic = "/ˈsɜː.vər/",
                    example = "Restart the server tonight."
                ),
                Flashcard(
                    word = "Token",
                    definition = "A unit of authentication or text.",
                    phonetic = "/ˈtəʊ.kən/",
                    example = "Paste your API token here."
                ),
                Flashcard(
                    word = "Uptime",
                    definition = "The time a system stays running.",
                    phonetic = "/ˈʌp.taɪm/",
                    example = "99.9% uptime is the goal."
                ),
                Flashcard(
                    word = "Version",
                    definition = "A specific release of software.",
                    phonetic = "/ˈvɜː.ʃən/",
                    example = "Update to the latest version."
                ),
                Flashcard(
                    word = "Widget",
                    definition = "A small UI component.",
                    phonetic = "/ˈwɪdʒ.ɪt/",
                    example = "Add a search widget at home."
                ),
                Flashcard(
                    word = "Syntax",
                    definition = "The rules of code structure.",
                    phonetic = "/ˈsɪn.tæks/",
                    example = "Check the syntax carefully."
                ),
                Flashcard(
                    word = "Boolean",
                    definition = "A true/false value type.",
                    phonetic = "/ˈbuː.li.ən/",
                    example = "Return a boolean flag."
                ),
                Flashcard(
                    word = "Frontend",
                    definition = "The user-facing part of software.",
                    phonetic = "/ˈfrʌnt.end/",
                    example = "She works on frontend design."
                )
            )

            else -> "Sample Flashcards" to listOf(
                Flashcard(
                    word = "Apple",
                    definition = "A round red or green fruit with firm white flesh.",
                    phonetic = "/ˈæp.əl/"
                ),
                Flashcard(
                    word = "Banana",
                    definition = "A long curved fruit with a yellow skin.",
                    phonetic = "/bəˈnɑː.nə/"
                ),
                Flashcard(
                    word = "Cherry",
                    definition = "A small, round, bright or dark red fruit.",
                    phonetic = "/ˈtʃer.i/"
                )
            )
        }
    }
}
