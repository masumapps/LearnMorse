package com.learn.morse.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.learn.morse.R

/** Short right/wrong answer sounds. SoundPool keeps them decoded in memory so they start without delay. */
class FeedbackSounds(context: Context, private val volume: Float) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val right = pool.load(context, R.raw.right, 1)
    private val wrong = pool.load(context, R.raw.wrong, 1)

    fun play(correct: Boolean) {
        pool.play(if (correct) right else wrong, volume, volume, 1, 0, 1f)
    }

    fun release() = pool.release()

    companion object {
        /** Length of wrong.mp3; the morse replay after a wrong answer waits for it to finish. */
        const val WRONG_DURATION_MS = 1750L
    }
}
