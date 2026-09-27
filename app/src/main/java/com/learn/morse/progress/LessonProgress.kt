package com.learn.morse.progress

import android.content.Context
import com.learn.morse.ui.screen.lessons

/** Which lessons the user has finished, persisted in SharedPreferences. Lessons unlock in order. */
object LessonProgress {
    private const val KEY_COMPLETED_COUNT = "completed_lesson_count"

    /** Number of lessons completed, counted from the start of [lessons]. */
    fun completedCount(context: Context): Int =
        prefs(context).getInt(KEY_COMPLETED_COUNT, 0).coerceIn(0, lessons.size)

    fun markCompleted(context: Context, lessonId: Long) {
        val count = lessons.indexOfFirst { it.id == lessonId } + 1
        val edit = prefs(context).edit().remove(stepKey(lessonId))
        // Replaying an earlier lesson must not roll progress back.
        if (count > completedCount(context)) edit.putInt(KEY_COMPLETED_COUNT, count)
        edit.apply()
    }

    /** Index of the step the user last reached in [lessonId], or 0 if it hasn't been started. */
    fun savedStep(context: Context, lessonId: Long): Int = prefs(context).getInt(stepKey(lessonId), 0)

    fun saveStep(context: Context, lessonId: Long, step: Int) {
        prefs(context).edit().putInt(stepKey(lessonId), step).apply()
    }

    private fun stepKey(lessonId: Long) = "lesson_step_$lessonId"

    private fun prefs(context: Context) =
        context.getSharedPreferences("learn_morse", Context.MODE_PRIVATE)
}
