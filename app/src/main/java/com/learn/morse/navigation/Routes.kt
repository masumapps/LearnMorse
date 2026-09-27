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
data object Learn
