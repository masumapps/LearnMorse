package com.learn.morse.ui.screen

val lessons = listOf(
    LessonModel(1, "E and T", "E,T", 20),
    LessonModel(2, "A and N", "A,N", 20),
    LessonModel(3, "E, T, A and N", "E,T,A,N", 20),
    LessonModel(4, "I and M", "I,M", 20),
    LessonModel(5, "Review: E, T, A, N, I and M", "E,T,A,N,I,M", 30),
    LessonModel(6, "S and O", "S,O", 20),
    LessonModel(7, "Review: E, T, A, N, I, M, S and O", "E,T,A,N,I,M,S,O", 40),
)

fun learnsFor(lessonId: Long): List<LearnModel> = when (lessonId) {
    1L -> pairLesson(lessonId, "E", "T")
    2L -> pairLesson(lessonId, "A", "N")
    3L -> buildLearns(lessonId) {
        // Each letter three times by sound, then twice from memory: receiving gets the larger share.
        listOf("A", "E", "N", "T", "E", "A", "T", "N", "T", "A", "E", "N").forEach { listen(it, isHidden = true) }
        listOf("N", "A", "T", "E", "A", "N", "E", "T").forEach { tap(it, isMorseHidden = true) }
    }
    4L -> pairLesson(lessonId, "I", "M")
    5L -> buildLearns(lessonId) {
        // Each letter three times by sound, then twice from memory; similar-sounding pairs (I/E, M/T) sit close together.
        listOf("I", "E", "M", "T", "A", "N", "E", "I", "N", "M", "T", "A", "M", "E", "A", "I", "T", "N").forEach { listen(it, isHidden = true) }
        listOf("M", "I", "T", "E", "N", "A", "I", "M", "A", "E", "N", "T").forEach { tap(it, isMorseHidden = true) }
    }
    6L -> pairLesson(lessonId, "S", "O")
    7L -> buildLearns(lessonId) {
        // Each letter three times by sound, then twice from memory; the all-dot (E/I/S) and all-dash (T/M/O) families sit close together.
        listOf("S", "I", "O", "M", "E", "T", "A", "N", "O", "S", "N", "A", "I", "E", "M", "T", "E", "O", "A", "S", "T", "I", "N", "M").forEach { listen(it, isHidden = true) }
        listOf("O", "S", "T", "E", "I", "M", "A", "N", "S", "O", "M", "I", "N", "T", "E", "A").forEach { tap(it, isMorseHidden = true) }
    }
    else -> emptyList()
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

private class LearnsBuilder(private val lessonId: Long) {
    val learns = mutableListOf<LearnModel>()

    fun listen(target: String, isHidden: Boolean = false) = add(target, isListen = true, isHidden = isHidden)

    fun tap(target: String, isMorseHidden: Boolean = false) = add(target, isListen = false, isMorseHidden = isMorseHidden)

    private fun add(target: String, isListen: Boolean, isHidden: Boolean = false, isMorseHidden: Boolean = false) {
        val position = learns.size
        learns += LearnModel(
            id = position + 1L,
            lessonId = lessonId,
            position = position,
            target = target,
            morse = morseMap.getValue(target),
            isListen = isListen,
            isLocked = false,
            isHidden = isHidden,
            isMorseHidden = isMorseHidden
        )
    }
}

private fun buildLearns(lessonId: Long, block: LearnsBuilder.() -> Unit): List<LearnModel> =
    LearnsBuilder(lessonId).apply(block).learns
