package com.learn.morse.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonDataTest {

    @Test
    fun coursesAreAlphabetNumbersPunctuation() {
        assertEquals(listOf("Alphabet", "Numbers", "Punctuation", "Signals"), courses.map { it.name })
    }

    @Test
    fun everyCourseTeachesItsSymbols() {
        fun taught(course: Course) =
            course.lessons.flatMap { learnsFor(it.id) }.filterNot { it.isWord }.map { it.target }.toSet()
        assertEquals(('A'..'Z').map { it.toString() }.toSet(), taught(courses[0]))
        assertEquals(('0'..'9').map { it.toString() }.toSet(), taught(courses[1]))
        // Punctuation covers every remaining code the translator knows.
        assertEquals(morseMap.keys - taught(courses[0]) - taught(courses[1]), taught(courses[2]))
        assertEquals(signalGroups.flatMap { it.second }.map { it.name }.toSet(), taught(courses[3]))
    }

    @Test
    fun prosignsRunTheirLettersTogetherAndOtherSignalsKeepGaps() {
        assertEquals("...---...", signalOf("SOS")!!.morse)
        assertEquals("........", signalOf("HH")!!.morse)
        assertEquals("-.-. --.-", signalOf("CQ")!!.morse)
        // Prosigns that share a punctuation code must match it.
        assertEquals(morseMap["+"], signalOf("AR")!!.morse)
        assertEquals(morseMap["="], signalOf("BT")!!.morse)
        assertEquals(morseMap["&"], signalOf("AS")!!.morse)
        assertEquals(morseMap["("], signalOf("KN")!!.morse)
    }

    @Test
    fun signalMeaningsAreUniqueSoEveryMeaningQuizHasOneAnswer() {
        val meanings = signalGroups.flatMap { it.second }.map { it.meaning }
        assertEquals(meanings.size, meanings.toSet().size)
    }

    @Test
    fun signalLessonsQuizEverySignalsMeaning() {
        courses[3].lessons.forEach { lesson ->
            assertEquals(LessonKind.Signals, lesson.kind)
            val steps = learnsFor(lesson.id)
            lesson.options.forEach { name ->
                val meaningSteps = steps.filter { it.isMeaning && it.target == name }
                assertEquals("${lesson.name}: $name", 2, meaningSteps.size)
                meaningSteps.forEach { assertEquals(signalOf(name)!!.meaning, it.meaning) }
                assertTrue(steps.filter { it.target == name }.all { it.morse == signalOf(name)!!.morse })
            }
        }
    }

    @Test
    fun signalsCourseHasAReviewAfterEverySecondGroup() {
        assertEquals(
            listOf("Making a call", "Replying", "Review 1", "Help and errors", "Q-codes", "Review 2"),
            courses[3].lessons.map { it.name },
        )
    }

    @Test
    fun alphabetKeepsItsLessonIdsSoSavedProgressStaysValid() {
        assertEquals((1L..28L).toList(), courses[0].lessons.filterNot { it.isWords }.map { it.id })
    }

    @Test
    fun letterLessonIdsContinueAcrossCoursesAndWordLessonsHaveTheirOwn() {
        val (words, symbols) = lessons.partition { it.isWords }
        assertEquals((1L..symbols.size).toList(), symbols.map { it.id })
        assertEquals((1001L until 1001L + words.size).toList(), words.map { it.id })
    }

    @Test
    fun wordLessonsFollowEveryThirdAlphabetReview() {
        val alphabet = courses[0].lessons
        val words = alphabet.filter { it.isWords }
        assertEquals(listOf("Words 1", "Words 2", "Words 3", "Words 4"), words.map { it.name })
        words.forEachIndexed { index, lesson ->
            // Directly after Review 3, 6, 9 or 12, or the Big Review that follows it.
            val before = alphabet[alphabet.indexOf(lesson) - 1].name
            assertTrue(lesson.name, before == "Review ${(index + 1) * 3}" || before.startsWith("Big Review"))
        }
        assertTrue(courses.drop(1).flatMap { it.lessons }.none { it.isWords })
    }

    @Test
    fun wordsOnlyUseLettersAlreadyLearned() {
        val learned = mutableSetOf<String>()
        courses[0].lessons.forEach { lesson ->
            if (lesson.isWords) {
                lesson.options.forEach { word ->
                    assertEquals(2, word.length)
                    word.forEach { assertTrue("${lesson.name}: $word", it.toString() in learned) }
                }
            } else {
                learned += lesson.options
            }
        }
    }

    @Test
    fun wordStepsCarryLetterGapsInTheirMorse() {
        val step = lessons.first { it.isWords }.let { learnsFor(it.id) }.first { it.target == "IT" }
        assertEquals(".. -", step.morse)
    }

    @Test
    fun wordLessonsHearEachWordTwiceThenTapIt() {
        lessons.filter { it.isWords }.forEach { lesson ->
            val steps = learnsFor(lesson.id)
            val count = lesson.options.size
            assertTrue(steps.take(count).all { it.isListen && !it.isHidden })
            assertTrue(steps.drop(count).take(count).all { it.isListen && it.isHidden })
            assertTrue(steps.drop(2 * count).all { !it.isListen && it.isMorseHidden })
            lesson.options.forEach { word -> assertEquals(3, steps.count { it.target == word }) }
        }
    }

    @Test
    fun lessonNumbersRestartInEachCourse() {
        courses.forEach { course ->
            assertEquals((1..course.lessons.size).toList(), course.lessons.map { lessonNumber(it) })
        }
    }

    @Test
    fun stepCountsMatchLessonModels() {
        lessons.forEach { lesson ->
            assertEquals("lesson ${lesson.id}", lesson.chapter, learnsFor(lesson.id).size)
        }
    }

    @Test
    fun targetsAreAmongTheLessonOptions() {
        lessons.forEach { lesson ->
            val options = lesson.options
            learnsFor(lesson.id).forEach { assertTrue("lesson ${lesson.id}: ${it.target}", it.target in options) }
        }
    }

    @Test
    fun answerGridsStaySmall() {
        lessons.forEach { assertTrue("lesson ${it.id}", it.options.size <= 12) }
    }

    @Test
    fun reviewsNeverRepeatALetterBackToBack() {
        lessons.filter { it.options.size > 2 }.forEach { lesson ->
            learnsFor(lesson.id).zipWithNext().forEach { (a, b) ->
                assertNotEquals("lesson ${lesson.id} step ${b.position}", a.target, b.target)
            }
        }
    }

    @Test
    fun reviewOrderIsStableAcrossCalls() {
        lessons.forEach { assertEquals(learnsFor(it.id), learnsFor(it.id)) }
    }

    @Test
    fun reviewsCoverTheLastSixLettersAndEveryThirdAddsTheLastTwelveOnceLearned() {
        val lessons = courses[0].lessons.filterNot { it.isWords }
        val learned = mutableListOf<String>()
        var reviews = 0
        var index = 0
        while (index < lessons.size) {
            learned += lessons[index].options
            index++
            if (learned.size == 2) continue
            reviews++
            assertEquals("Review $reviews", lessons[index].name)
            assertEquals(learned.takeLast(6).toSet(), lessons[index].options.toSet())
            index++
            if (reviews % 3 == 0 && learned.size >= 12) {
                assertTrue(lessons[index].name.startsWith("Big Review"))
                assertEquals(learned.takeLast(12).toSet(), lessons[index].options.toSet())
                index++
            }
        }
        assertEquals(12, reviews)
    }
}
