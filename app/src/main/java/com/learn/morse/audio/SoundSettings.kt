package com.learn.morse.audio

import android.content.Context

/**
 * How morse is played for the user, persisted in SharedPreferences: the tone, the speed, and which
 * outputs carry the signal. Outputs other than sound let deaf and hard-of-hearing users learn too.
 */
data class SoundSettings(
    val frequency: Int = DEFAULT_FREQUENCY,
    val wpm: Int = DEFAULT_WPM,
    val volume: Float = DEFAULT_VOLUME,
    // Farnsworth: letters keep [wpm] but the gaps between them stretch to this overall speed. Null is off.
    val farnsworthWpm: Int? = null,
    val sound: Boolean = true,
    val vibrate: Boolean = false,
    val screenFlash: Boolean = false,
    // Premium only; MorsePlayer ignores it otherwise.
    val flashlight: Boolean = false,
) {
    // PARIS standard: one word is 50 units, so a unit lasts 1200 / wpm milliseconds.
    val unitMs: Int get() = 1200 / wpm

    /**
     * The unit for gaps between letters (3 of them) and words (7). Without Farnsworth it equals [unitMs];
     * with it, ARRL's formula spreads the extra time over the 19 gap units of PARIS.
     */
    val gapUnitMs: Int
        get() {
            val effective = farnsworthWpm?.takeIf { it < wpm } ?: return unitMs
            return ((60_000L * wpm - 37_200L * effective) / (19L * effective * wpm)).toInt()
        }

    fun save(context: Context) {
        prefs(context).edit()
            .putInt(KEY_FREQUENCY, frequency)
            .putInt(KEY_WPM, wpm)
            .putFloat(KEY_VOLUME, volume)
            .putInt(KEY_FARNSWORTH_WPM, farnsworthWpm ?: 0)
            .putBoolean(KEY_SOUND, sound)
            .putBoolean(KEY_VIBRATE, vibrate)
            .putBoolean(KEY_SCREEN_FLASH, screenFlash)
            .putBoolean(KEY_FLASHLIGHT, flashlight)
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
        private const val KEY_FARNSWORTH_WPM = "sound_farnsworth_wpm"
        private const val KEY_SOUND = "output_sound"
        private const val KEY_VIBRATE = "output_vibrate"
        private const val KEY_SCREEN_FLASH = "output_screen_flash"
        private const val KEY_FLASHLIGHT = "output_flashlight"

        fun load(context: Context): SoundSettings {
            val prefs = prefs(context)
            return SoundSettings(
                frequency = prefs.getInt(KEY_FREQUENCY, DEFAULT_FREQUENCY),
                wpm = prefs.getInt(KEY_WPM, DEFAULT_WPM),
                volume = prefs.getFloat(KEY_VOLUME, DEFAULT_VOLUME),
                farnsworthWpm = prefs.getInt(KEY_FARNSWORTH_WPM, 0).takeIf { it > 0 },
                sound = prefs.getBoolean(KEY_SOUND, true),
                vibrate = prefs.getBoolean(KEY_VIBRATE, false),
                screenFlash = prefs.getBoolean(KEY_SCREEN_FLASH, false),
                flashlight = prefs.getBoolean(KEY_FLASHLIGHT, false),
            )
        }

        private fun prefs(context: Context) =
            context.getSharedPreferences("learn_morse", Context.MODE_PRIVATE)
    }
}
