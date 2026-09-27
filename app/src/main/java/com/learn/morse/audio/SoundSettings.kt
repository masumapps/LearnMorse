package com.learn.morse.audio

import android.content.Context

/** User-configurable sound for morse playback and the tap key, persisted in SharedPreferences. */
data class SoundSettings(
    val frequency: Int = DEFAULT_FREQUENCY,
    val wpm: Int = DEFAULT_WPM,
    val volume: Float = DEFAULT_VOLUME,
) {
    // PARIS standard: one word is 50 units, so a unit lasts 1200 / wpm milliseconds.
    val unitMs: Int get() = 1200 / wpm

    fun save(context: Context) {
        prefs(context).edit()
            .putInt(KEY_FREQUENCY, frequency)
            .putInt(KEY_WPM, wpm)
            .putFloat(KEY_VOLUME, volume)
            .apply()
    }

    companion object {
        const val DEFAULT_FREQUENCY = 800
        const val DEFAULT_WPM = 20
        const val DEFAULT_VOLUME = 0.8f
        val FREQUENCY_RANGE = 400..1200
        val WPM_RANGE = 5..40

        private const val KEY_FREQUENCY = "sound_frequency"
        private const val KEY_WPM = "sound_wpm"
        private const val KEY_VOLUME = "sound_volume"

        fun load(context: Context): SoundSettings {
            val prefs = prefs(context)
            return SoundSettings(
                frequency = prefs.getInt(KEY_FREQUENCY, DEFAULT_FREQUENCY),
                wpm = prefs.getInt(KEY_WPM, DEFAULT_WPM),
                volume = prefs.getFloat(KEY_VOLUME, DEFAULT_VOLUME),
            )
        }

        private fun prefs(context: Context) =
            context.getSharedPreferences("learn_morse", Context.MODE_PRIVATE)
    }
}
