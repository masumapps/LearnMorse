package com.learn.morse.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data class Lesson(
    val id: Int,
    // Resume from the saved step instead of the first one.
    val resume: Boolean = false,
)

@Serializable
data object Settings

/** The course menu: Alphabet, Numbers, Punctuation. */
@Serializable
data object Learn

/** The lesson list of one course. */
@Serializable
data class CourseLessons(val courseId: Int)

@Serializable
data object Translator
