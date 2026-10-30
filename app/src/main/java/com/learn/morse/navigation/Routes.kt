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

@Serializable
data object PrivacyPolicy

/** The course menu: Alphabet, Numbers, Punctuation. */
@Serializable
data object Learn

/** The lesson list of one course. */
@Serializable
data class CourseLessons(val courseId: Int)

@Serializable
data object Translator

/** The searchable chart of every symbol and signal. */
@Serializable
data object Chart

/** A short session of the symbols missed most lately. */
@Serializable
data object Practice

/** First-run onboarding: language, goal, signal outputs, reminder and Premium. */
@Serializable
data object Welcome

/** The Premium offer, opened from the crown on Home. */
@Serializable
data object Premium
