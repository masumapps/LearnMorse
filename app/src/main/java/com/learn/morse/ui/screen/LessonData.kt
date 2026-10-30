package com.learn.morse.ui.screen

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.learn.morse.R
import kotlin.random.Random

/** A group of lessons on the Learn menu. Each course unlocks once every lesson of the one before is finished. */
data class Course(
    val id: Int,
    @StringRes val name: Int,
    @StringRes val description: Int,
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
        name: Int,
        description: Int,
        badge: String,
        pairs: List<Pair<String, String>>,
        words: List<List<String>> = emptyList(),
    ) {
        add(Course(size + 1, name, description, badge, courseLessons(ids, pairs, words)))
    }
    course(R.string.course_alphabet, R.string.course_alphabet_desc, "Ab", alphabetPairs, alphabetWords)
    course(R.string.course_numbers, R.string.course_numbers_desc, "12", numberPairs)
    course(R.string.course_punctuation, R.string.course_punctuation_desc, "?!", punctuationPairs)
    add(Course(size + 1, R.string.course_signals, R.string.course_signals_desc, "SOS", signalLessons(ids)))
    add(Course(size + 1, R.string.course_words, R.string.course_words_desc, "Hi", wordsCourseLessons()))
    add(Course(size + 1, R.string.course_on_air, R.string.course_on_air_desc, "QSO", onAirLessons()))
    add(Course(size + 1, R.string.course_speed, R.string.course_speed_desc, "⚡", speedLessons()))
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

/** What a lesson is called, kept as data so it's put into words in the user's language when shown. */
sealed interface LessonTitle {
    data class Pair(val a: String, val b: String) : LessonTitle
    data class Review(val number: Int) : LessonTitle
    data class BigReview(val number: Int) : LessonTitle
    data class Words(val number: Int) : LessonTitle
    // In the Words & Phrases course: "3-letter words 2".
    data class WordLength(val letters: Int, val number: Int) : LessonTitle
    data class Phrases(val number: Int) : LessonTitle
    // In the On the Air course.
    data class Callsigns(val number: Int) : LessonTitle
    data class Exchanges(val number: Int) : LessonTitle
    // In the Speed course: what's copied, at what speed.
    data class SpeedLetters(val wpm: Int) : LessonTitle
    data class SpeedWords(val wpm: Int) : LessonTitle
    data class SpeedPhrases(val wpm: Int) : LessonTitle
    // A signals lesson, named after its group.
    data class Group(@StringRes val name: Int) : LessonTitle
    data class Practice(val hasWeak: Boolean) : LessonTitle
}

/** The lesson's name, like "E and T" or "Review 2". */
@Composable
fun LessonModel.displayName(): String = when (val t = title) {
    is LessonTitle.Pair -> stringResource(R.string.lesson_title_pair, t.a, t.b)
    is LessonTitle.Review -> stringResource(R.string.lesson_title_review, t.number)
    is LessonTitle.BigReview -> stringResource(R.string.lesson_title_big_review, t.number)
    is LessonTitle.Words -> stringResource(R.string.lesson_title_words, t.number)
    is LessonTitle.WordLength -> stringResource(R.string.lesson_title_word_length, t.letters, t.number)
    is LessonTitle.Phrases -> stringResource(R.string.lesson_title_phrases, t.number)
    is LessonTitle.Callsigns -> stringResource(R.string.lesson_title_callsigns, t.number)
    is LessonTitle.Exchanges -> stringResource(R.string.lesson_title_exchanges, t.number)
    is LessonTitle.SpeedLetters -> stringResource(R.string.lesson_title_speed_letters, t.wpm)
    is LessonTitle.SpeedWords -> stringResource(R.string.lesson_title_speed_words, t.wpm)
    is LessonTitle.SpeedPhrases -> stringResource(R.string.lesson_title_speed_phrases, t.wpm)
    is LessonTitle.Group -> stringResource(t.name)
    is LessonTitle.Practice -> stringResource(if (t.hasWeak) R.string.lesson_title_practice else R.string.lesson_title_quick_review)
}

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
    fun lesson(title: LessonTitle, symbols: List<String>) {
        // Pair lessons are 20 steps; reviews take each symbol five times.
        val steps = if (symbols.size == 2) 20 else symbols.size * 5
        add(LessonModel(ids.nextSymbolId++, title, symbols, steps))
    }
    fun wordLesson(number: Int, words: List<String>) {
        add(LessonModel(ids.nextWordId++, LessonTitle.Words(number), words, words.size * WORD_ROUNDS, LessonKind.Words))
    }
    fun recent(learned: List<Pair<String, String>>, count: Int) =
        learned.takeLast(count / 2).reversed().flatMap { listOf(it.first, it.second) }

    var reviews = 0
    var bigReviews = 0
    pairs.forEachIndexed { index, (a, b) ->
        lesson(LessonTitle.Pair(a, b), listOf(a, b))
        if (index == 0) return@forEachIndexed
        val learned = pairs.take(index + 1)
        reviews++
        lesson(LessonTitle.Review(reviews), recent(learned, 6))
        if (reviews % 3 == 0 && learned.size * 2 >= 12) lesson(LessonTitle.BigReview(++bigReviews), recent(learned, 12))
        if (reviews % 3 == 0) words.getOrNull(reviews / 3 - 1)?.let { wordLesson(reviews / 3, it) }
    }
}

fun learnsFor(lessonId: Long): List<LearnModel> {
    // The later courses: speed drills, then words, callsigns and exchanges. Their signal-style
    // abbreviation lessons go on to the generic signal lessons below.
    val lesson = lessons.firstOrNull { it.id == lessonId }
    if (lesson != null && lessonId >= FIRST_WORDS_COURSE_ID && lesson.kind == LessonKind.Words) {
        return if (lesson.wpm != null) speedLearns(lessonId, lesson.options)
        else wordsCourseLearns(lessonId, lesson.options, isReview = lesson.title is LessonTitle.Review)
    }
    return learnsForCourseLesson(lessonId)
}

private fun learnsForCourseLesson(lessonId: Long): List<LearnModel> = when (lessonId) {
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
    fun lesson(title: LessonTitle, signals: List<String>, rounds: Int) {
        add(LessonModel(ids.nextSymbolId++, title, signals, signals.size * rounds, LessonKind.Signals))
    }
    signalGroups.forEachIndexed { index, (name, signals) ->
        lesson(LessonTitle.Group(name), signals.map { it.name }, SIGNAL_ROUNDS)
        if (index % 2 == 1) {
            val recent = signalGroups.subList(index - 1, index + 1).flatMap { group -> group.second.map { it.name } }
            lesson(LessonTitle.Review(index / 2 + 1), recent, SIGNAL_REVIEW_ROUNDS)
        }
    }
}

/**
 * New signals: hear each shown, pick it by sound twice, pick its meaning by sound twice, tap it with
 * the code shown, then from memory. A review keeps one of each quiz and the tap from memory.
 */
internal fun signalLesson(lessonId: Long, signals: List<String>, isReview: Boolean) = buildLearns(lessonId, isSignals = true) {
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
internal fun shuffledRounds(lessonId: Long, items: List<String>): () -> List<String> {
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

internal class LearnsBuilder(private val lessonId: Long, private val isSignals: Boolean) {
    val learns = mutableListOf<LearnModel>()

    fun listen(target: String, isHidden: Boolean = false, choices: List<String>? = null) =
        add(target, isListen = true, isHidden = isHidden, choices = choices)

    /** Hear the signal, hidden, and pick what it means. */
    fun meaning(target: String) = add(target, isListen = true, isHidden = true, isMeaning = true)

    fun tap(target: String, isMorseHidden: Boolean = false) = add(target, isListen = false, isMorseHidden = isMorseHidden)

    private fun add(
        target: String,
        isListen: Boolean,
        isHidden: Boolean = false,
        isMorseHidden: Boolean = false,
        isMeaning: Boolean = false,
        choices: List<String>? = null,
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
            choices = choices,
        )
    }
}

internal fun buildLearns(lessonId: Long, isSignals: Boolean = false, block: LearnsBuilder.() -> Unit): List<LearnModel> =
    LearnsBuilder(lessonId, isSignals).apply(block).learns

/** Id of the practice session, which isn't a lesson of any course and saves no progress. */
const val PRACTICE_LESSON_ID = -1L

val LessonModel.isPractice: Boolean get() = id == PRACTICE_LESSON_ID

private const val PRACTICE_SYMBOLS = 6
private const val PRACTICE_LISTEN_STEPS = 10
private const val PRACTICE_TAP_STEPS = 6

/** Letters, digits and marks taught by the first [lessonsDone] lessons, not counting words and signals. */
fun learnedSymbols(lessonsDone: Int): List<String> =
    lessons.take(lessonsDone).filter { it.kind == LessonKind.Symbols }.flatMap { it.options }.distinct()

/**
 * A short practice of the symbols missed most lately ([weak], most missed first), topped up with other
 * [learned] ones so there are enough answers to choose from. Weaker symbols come up more often.
 * Recognizing by sound first, then tapping from memory. Null until at least two symbols are learned.
 */
fun practiceSession(learned: List<String>, weak: List<String>, random: Random): Pair<LessonModel, List<LearnModel>>? {
    if (learned.size < 2) return null
    val picked = (weak.take(PRACTICE_SYMBOLS) + learned.filterNot { it in weak }.shuffled(random))
        .take(PRACTICE_SYMBOLS.coerceAtMost(learned.size))
    // Weak symbols twice as likely as the ones topping up the set.
    val pool = picked.flatMap { if (it in weak) listOf(it, it) else listOf(it) }
    fun draw(count: Int): List<String> = buildList {
        while (size < count) {
            val next = pool.random(random)
            if (next != lastOrNull()) add(next)
        }
    }
    val learns = buildLearns(PRACTICE_LESSON_ID) {
        draw(PRACTICE_LISTEN_STEPS).forEach { listen(it, isHidden = true) }
        draw(PRACTICE_TAP_STEPS).forEach { tap(it, isMorseHidden = true) }
    }
    return LessonModel(PRACTICE_LESSON_ID, LessonTitle.Practice(hasWeak = weak.isNotEmpty()), picked, learns.size) to learns
}
