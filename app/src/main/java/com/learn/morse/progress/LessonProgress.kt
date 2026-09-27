package com.learn.morse.progress

import android.content.Context
import com.learn.morse.ui.screen.Course
import com.learn.morse.ui.screen.lessons

/** Which lessons the user has finished, persisted in SharedPreferences. Lessons unlock in order. */
object LessonProgress {
    // The furthest lesson finished. Stored by id, not as a count, so lessons inserted later
    // (like the word lessons) don't shift the user's place.
    private const val KEY_LAST_COMPLETED_ID = "last_completed_lesson_id"
    // Before word lessons: the number of lessons finished, whose ids were 1 to that number.
    private const val KEY_LEGACY_COMPLETED_COUNT = "completed_lesson_count"

    /**
     * Number of lessons completed, counted from the start of [lessons]. Lessons inserted before
     * the furthest one finished count as completed, so they don't lock anything already reached.
     */
    fun completedCount(context: Context): Int {
        val prefs = prefs(context)
        val lastId = if (prefs.contains(KEY_LAST_COMPLETED_ID)) prefs.getLong(KEY_LAST_COMPLETED_ID, 0)
        else prefs.getInt(KEY_LEGACY_COMPLETED_COUNT, 0).toLong()
        return lessons.indexOfFirst { it.id == lastId } + 1
    }

    /** Lessons of [course] completed, counted from its first lesson. */
    fun completedIn(context: Context, course: Course): Int =
        (completedCount(context) - lessons.indexOf(course.lessons.first())).coerceIn(0, course.lessons.size)

    /** A course unlocks once every lesson before it, i.e. every earlier course, is finished. */
    fun isUnlocked(context: Context, course: Course): Boolean =
        completedCount(context) >= lessons.indexOf(course.lessons.first())

    /** Records a finished lesson and its [stars]; only the best star count is kept. */
    fun markCompleted(context: Context, lessonId: Long, stars: Int) {
        val count = lessons.indexOfFirst { it.id == lessonId } + 1
        val edit = prefs(context).edit()
            .remove(stepKey(lessonId))
            .remove(mistakesKey(lessonId))
        // Replaying an earlier lesson must not roll progress back.
        if (count > completedCount(context)) edit.putLong(KEY_LAST_COMPLETED_ID, lessonId)
        if (stars > stars(context, lessonId)) edit.putInt(starsKey(lessonId), stars)
        edit.apply()
    }

    /** Best stars earned in [lessonId]: 1-3, or 0 if it was never finished with stars. */
    fun stars(context: Context, lessonId: Long): Int = prefs(context).getInt(starsKey(lessonId), 0)

    /**
     * Stars for a finished lesson, relative to its length so long reviews aren't harder to ace:
     * 3 for mistakes on at most 10% of the steps, 2 for at most 25%, otherwise 1.
     */
    fun starsFor(mistakes: Int, stepCount: Int): Int = when {
        mistakes * 10 <= stepCount -> 3
        mistakes * 4 <= stepCount -> 2
        else -> 1
    }

    /** Wrong answers so far in an unfinished [lessonId], so resuming keeps counting toward its stars. */
    fun savedMistakes(context: Context, lessonId: Long): Int = prefs(context).getInt(mistakesKey(lessonId), 0)

    fun saveMistakes(context: Context, lessonId: Long, mistakes: Int) {
        prefs(context).edit().putInt(mistakesKey(lessonId), mistakes).apply()
    }

    /** Index of the step the user last reached in [lessonId], or 0 if it hasn't been started. */
    fun savedStep(context: Context, lessonId: Long): Int = prefs(context).getInt(stepKey(lessonId), 0)

    fun saveStep(context: Context, lessonId: Long, step: Int) {
        prefs(context).edit().putInt(stepKey(lessonId), step).apply()
    }

    private fun stepKey(lessonId: Long) = "lesson_step_$lessonId"
    private fun mistakesKey(lessonId: Long) = "lesson_mistakes_$lessonId"
    private fun starsKey(lessonId: Long) = "lesson_stars_$lessonId"

    private fun prefs(context: Context) =
        context.getSharedPreferences("learn_morse", Context.MODE_PRIVATE)
}
