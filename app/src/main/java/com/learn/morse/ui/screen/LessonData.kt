package com.learn.morse.ui.screen

import kotlin.random.Random

/** A group of lessons on the Learn menu. Each course unlocks once every lesson of the one before is finished. */
data class Course(
    val id: Int,
    val name: String,
    val description: String,
    // A short sample of the course's symbols, shown in its badge.
    val badge: String,
    val lessons: List<LessonModel>,
)

/**
 * The order symbols are taught in, two per lesson. Numbers start with the all-dot 5 and all-dash 0,
 * then go in order; punctuation starts with the most used marks.
 */
private val alphabetPairs = listOf(
    "E" to "T", "A" to "N", "I" to "M", "S" to "O", "R" to "K", "D" to "U", "G" to "W",
    "H" to "V", "B" to "F", "L" to "P", "C" to "Y", "Q" to "Z", "J" to "X",
)
private val numberPairs = listOf("5" to "0", "1" to "2", "3" to "4", "6" to "7", "8" to "9")
private val punctuationPairs = listOf(
    "." to ",", "?" to "/", "=" to "+", "-" to "'", "!" to "@",
    "(" to ")", ":" to ";", "&" to "\"", "_" to "$",
)

/**
 * Two-letter words practised after every third alphabet review, using only letters learned by then:
 * E T A N I M S O, then + R K D U G W, + H V B F L P, and finally the whole alphabet.
 */
private val alphabetWords = listOf(
    listOf("AM", "AN", "AS", "AT", "IN", "IS", "IT", "ME", "NO", "ON", "SO", "TO"),
    listOf("DO", "GO", "OK", "OR", "US", "WE", "AT", "IN", "IS", "ME"),
    listOf("BE", "HE", "HI", "IF", "OF", "OH", "UP", "DO", "WE", "GO"),
    listOf("BY", "MY", "OX", "AX", "HE", "OF", "UP", "BE", "US", "OK"),
)

/**
 * Ids never change once a lesson exists, since stars and saved steps are stored by id. Letter and review
 * lessons are numbered on across courses (the alphabet keeps 1-28); word lessons, added later in
 * between them, number from [FIRST_WORD_LESSON_ID].
 */
val courses: List<Course> = buildList {
    val ids = LessonIds()
    fun course(
        name: String,
        description: String,
        badge: String,
        pairs: List<Pair<String, String>>,
        words: List<List<String>> = emptyList(),
    ) {
        add(Course(size + 1, name, description, badge, courseLessons(ids, pairs, words)))
    }
    course("Alphabet", "A to Z", "Ab", alphabetPairs, alphabetWords)
    course("Numbers", "0 to 9", "12", numberPairs)
    course("Punctuation", ". , ? / and more", "?!", punctuationPairs)
    add(Course(size + 1, "Signals", "SOS, CQ, 73 and more", "SOS", signalLessons(ids)))
}

/** Every lesson of every course, in the order they unlock. */
val lessons: List<LessonModel> = courses.flatMap { it.lessons }

fun courseOf(lessonId: Long): Course = courses.first { course -> course.lessons.any { it.id == lessonId } }

/** The lesson's 1-based number within its course, shown as "Lesson N". */
fun lessonNumber(lesson: LessonModel): Int = courseOf(lesson.id).lessons.indexOfFirst { it.id == lesson.id } + 1

private const val FIRST_WORD_LESSON_ID = 1001L

private class LessonIds {
    var nextSymbolId = 1L
    var nextWordId = FIRST_WORD_LESSON_ID
}

val LessonModel.isWords: Boolean get() = kind == LessonKind.Words

/**
 * Every pair after the first is followed by a review of the last 6 symbols learned, and every
 * third review by a longer one of the last 12, once 12 have been learned. Reviews list the newest pair first.
 * The next list of [words], if any, follows every third review.
 */
private fun courseLessons(
    ids: LessonIds,
    pairs: List<Pair<String, String>>,
    words: List<List<String>>,
): List<LessonModel> = buildList {
    fun lesson(name: String, symbols: List<String>) {
        // Pair lessons are 20 steps; reviews take each symbol five times.
        val steps = if (symbols.size == 2) 20 else symbols.size * 5
        add(LessonModel(ids.nextSymbolId++, name, symbols, steps))
    }
    fun wordLesson(number: Int, words: List<String>) {
        add(LessonModel(ids.nextWordId++, "Words $number", words, words.size * WORD_ROUNDS, LessonKind.Words))
    }
    fun recent(learned: List<Pair<String, String>>, count: Int) =
        learned.takeLast(count / 2).reversed().flatMap { listOf(it.first, it.second) }

    var reviews = 0
    var bigReviews = 0
    pairs.forEachIndexed { index, (a, b) ->
        lesson("$a and $b", listOf(a, b))
        if (index == 0) return@forEachIndexed
        val learned = pairs.take(index + 1)
        reviews++
        lesson("Review $reviews", recent(learned, 6))
        if (reviews % 3 == 0 && learned.size * 2 >= 12) lesson("Big Review ${++bigReviews}", recent(learned, 12))
        if (reviews % 3 == 0) words.getOrNull(reviews / 3 - 1)?.let { wordLesson(reviews / 3, it) }
    }
}

fun learnsFor(lessonId: Long): List<LearnModel> = when (lessonId) {
    3L -> buildLearns(lessonId) {
        // Each letter three times by sound, then twice from memory: receiving gets the larger share.
        listOf("A", "E", "N", "T", "E", "A", "T", "N", "T", "A", "E", "N").forEach { listen(it, isHidden = true) }
        listOf("A", "N", "T", "E", "A", "N", "E", "T").forEach { tap(it, isMorseHidden = true) }
    }
    5L -> buildLearns(lessonId) {
        // Each letter three times by sound, then twice from memory; similar-sounding pairs (I/E, M/T) sit close together.
        listOf("I", "E", "M", "T", "A", "N", "E", "I", "N", "M", "T", "A", "M", "E", "A", "I", "T", "N").forEach { listen(it, isHidden = true) }
        listOf("M", "I", "T", "E", "N", "A", "I", "M", "A", "E", "N", "T").forEach { tap(it, isMorseHidden = true) }
    }
    // Later lessons are generated from their options: two letters make a pair lesson, more a review.
    else -> lessons.firstOrNull { it.id == lessonId }?.let { lesson ->
        val letters = lesson.options
        if (lesson.isWords) wordLesson(lessonId, letters)
        else if (lesson.kind == LessonKind.Signals) signalLesson(lessonId, letters, isReview = letters.size > SIGNALS_PER_LESSON)
        else if (letters.size == 2) pairLesson(lessonId, letters[0], letters[1]) else reviewLesson(lessonId, letters)
    } ?: emptyList()
}

private const val SIGNALS_PER_LESSON = 4

/** Rounds through a new signal lesson's signals; a review has fewer, as its signals are already known. */
private const val SIGNAL_ROUNDS = 7
private const val SIGNAL_REVIEW_ROUNDS = 4

/** A lesson for each group of signals, and a review after every second one. */
private fun signalLessons(ids: LessonIds): List<LessonModel> = buildList {
    fun lesson(name: String, signals: List<String>, rounds: Int) {
        add(LessonModel(ids.nextSymbolId++, name, signals, signals.size * rounds, LessonKind.Signals))
    }
    signalGroups.forEachIndexed { index, (name, signals) ->
        lesson(name, signals.map { it.name }, SIGNAL_ROUNDS)
        if (index % 2 == 1) {
            val recent = signalGroups.subList(index - 1, index + 1).flatMap { group -> group.second.map { it.name } }
            lesson("Review ${index / 2 + 1}", recent, SIGNAL_REVIEW_ROUNDS)
        }
    }
}

/**
 * New signals: hear each shown, pick it by sound twice, pick its meaning by sound twice, tap it with
 * the code shown, then from memory. A review keeps one of each quiz and the tap from memory.
 */
private fun signalLesson(lessonId: Long, signals: List<String>, isReview: Boolean) = buildLearns(lessonId, isSignals = true) {
    val round = shuffledRounds(lessonId, signals)
    if (!isReview) round().forEach { listen(it) }
    repeat(if (isReview) 1 else 2) { round().forEach { listen(it, isHidden = true) } }
    repeat(2) { round().forEach { meaning(it) } }
    if (!isReview) round().forEach { tap(it) }
    round().forEach { tap(it, isMorseHidden = true) }
}

/** Rounds through the words of a word lesson: hear it shown, hear it as a quiz, tap it from memory. */
private const val WORD_ROUNDS = 3

private fun wordLesson(lessonId: Long, words: List<String>) = buildLearns(lessonId) {
    val round = shuffledRounds(lessonId, words)
    round().forEach { listen(it) }
    round().forEach { listen(it, isHidden = true) }
    round().forEach { tap(it, isMorseHidden = true) }
}

/**
 * Returns a function giving [items] in a new shuffled order on each call, seeded with the lesson id.
 * The seed keeps the order identical across launches, so a saved step resumes on the same item.
 */
private fun shuffledRounds(lessonId: Long, items: List<String>): () -> List<String> {
    val random = Random(lessonId)
    var previous: String? = null
    return {
        var order = items.shuffled(random)
        // Never the same item twice in a row across the round boundary.
        while (order.first() == previous) order = items.shuffled(random)
        previous = order.last()
        order
    }
}

/** Every letter three times by sound, then twice from memory, each round in a new order. */
private fun reviewLesson(lessonId: Long, letters: List<String>) = buildLearns(lessonId) {
    val round = shuffledRounds(lessonId, letters)
    repeat(3) { round().forEach { listen(it, isHidden = true) } }
    repeat(2) { round().forEach { tap(it, isMorseHidden = true) } }
}

/**
 * Introduces two new letters: listen, guess by sound, tap with hint, tap from memory.
 * The sound quiz is twice as long as each tap block so receiving isn't outweighed by sending.
 */
private fun pairLesson(lessonId: Long, a: String, b: String) = buildLearns(lessonId) {
    listOf(a, a, b, b).forEach { listen(it) }
    listOf(a, b, b, a, b, a, a, b).forEach { listen(it, isHidden = true) }
    listOf(a, a, b, b).forEach { tap(it) }
    listOf(a, a, b, b).forEach { tap(it, isMorseHidden = true) }
}

private class LearnsBuilder(private val lessonId: Long, private val isSignals: Boolean) {
    val learns = mutableListOf<LearnModel>()

    fun listen(target: String, isHidden: Boolean = false) = add(target, isListen = true, isHidden = isHidden)

    /** Hear the signal, hidden, and pick what it means. */
    fun meaning(target: String) = add(target, isListen = true, isHidden = true, isMeaning = true)

    fun tap(target: String, isMorseHidden: Boolean = false) = add(target, isListen = false, isMorseHidden = isMorseHidden)

    private fun add(
        target: String,
        isListen: Boolean,
        isHidden: Boolean = false,
        isMorseHidden: Boolean = false,
        isMeaning: Boolean = false,
    ) {
        val signal = if (isSignals) signalOf(target) else null
        val position = learns.size
        learns += LearnModel(
            id = position + 1L,
            lessonId = lessonId,
            position = position,
            target = target,
            morse = signal?.morse ?: morseOf(target),
            isListen = isListen,
            isLocked = false,
            isHidden = isHidden,
            isMorseHidden = isMorseHidden,
            meaning = signal?.meaning,
            isMeaning = isMeaning,
        )
    }
}

private fun buildLearns(lessonId: Long, isSignals: Boolean = false, block: LearnsBuilder.() -> Unit): List<LearnModel> =
    LearnsBuilder(lessonId, isSignals).apply(block).learns
