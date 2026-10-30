package com.learn.morse.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonDataTest {
    @Test
    fun lessonIdsAreUnique() {
        assertEquals(lessons.size, lessons.map { it.id }.toSet().size)
    }

    @Test
    fun everyLessonHasStepsMatchingItsCount() {
        lessons.forEach { lesson ->
            val learns = learnsFor(lesson.id)
            assertTrue("lesson ${lesson.id} has no steps", learns.isNotEmpty())
            assertEquals("lesson ${lesson.id} step count", lesson.chapter, learns.size)
        }
    }

    @Test
    fun wordsCourseStepsAreAnswerable() {
        val course = courses.first { it.name == com.learn.morse.R.string.course_words }
        // 3-, 4- and 5-letter words and phrases, each group followed by a review.
        assertEquals(20, course.lessons.size)
        course.lessons.forEach { lesson ->
            learnsFor(lesson.id).forEach { step ->
                // Every character has a code, and phrases get word gaps.
                assertEquals(step.target.count { it == ' ' }, step.morse.count { it == '/' })
                if (step.isListen) {
                    val choices = step.choices!!
                    assertTrue(step.target in choices)
                    assertEquals(6, choices.size)
                    assertEquals(choices.size, choices.toSet().size)
                } else {
                    // Phrases are never tapped.
                    assertFalse(' ' in step.target)
                }
            }
        }
    }

    @Test
    fun morseOfSeparatesWords() {
        assertEquals("... --- / .. -", morseOf("SO IT"))
        assertEquals(".. -", morseOf("IT"))
    }

    @Test
    fun appHasOverOneHundredLessons() {
        assertTrue("only ${lessons.size} lessons", lessons.size >= 100)
    }

    @Test
    fun speedLessonsSetTheirSpeedAndOnlyListen() {
        val speed = courses.first { it.name == com.learn.morse.R.string.course_speed }
        speed.lessons.forEach { lesson ->
            assertTrue(lesson.wpm != null)
            assertTrue(learnsFor(lesson.id).all { it.isListen && it.isHidden && it.choices != null })
        }
    }

    @Test
    fun abbreviationLessonsTeachMeanings() {
        val onAir = courses.first { it.name == com.learn.morse.R.string.course_on_air }
        val abbreviations = onAir.lessons.filter { it.kind == LessonKind.Signals }
        assertEquals(5, abbreviations.size)
        abbreviations.forEach { lesson ->
            assertTrue(learnsFor(lesson.id).any { it.isMeaning && it.meaning != null })
        }
    }
}
