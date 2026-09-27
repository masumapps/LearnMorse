package com.learn.morse.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Plays a morse string made of '.', '-', ' ' (letter gap) and '/' (word gap) as a sine tone.
 * Cancelling the calling coroutine stops playback.
 */
object MorsePlayer {
    private const val SAMPLE_RATE = 44100
    private const val RAMP_MS = 5

    suspend fun play(morse: String, unitMs: Int = 60, frequency: Double = 800.0, volume: Float = 0.8f) {
        val pcm = withContext(Dispatchers.Default) { buildPcm(morse, unitMs, frequency, volume) }
        if (pcm.isEmpty()) return

        val builder = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        }
        val track = builder.build()

        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            delay(pcm.size * 1000L / SAMPLE_RATE)
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    private fun buildPcm(morse: String, unitMs: Int, frequency: Double, volume: Float): ShortArray {
        val unit = SAMPLE_RATE * unitMs / 1000
        val samples = ArrayList<Short>()

        fun tone(units: Int) {
            val length = unit * units
            val ramp = min(SAMPLE_RATE * RAMP_MS / 1000, length / 2)
            for (i in 0 until length) {
                // Short fade in/out avoids audible clicks at tone edges.
                val envelope = when {
                    i < ramp -> i.toDouble() / ramp
                    i > length - ramp -> (length - i).toDouble() / ramp
                    else -> 1.0
                }
                val value = sin(2 * PI * frequency * i / SAMPLE_RATE) * envelope * Short.MAX_VALUE * volume
                samples.add(value.toInt().toShort())
            }
        }

        fun silence(units: Int) = repeat(unit * units) { samples.add(0) }

        morse.forEach { symbol ->
            when (symbol) {
                '.' -> { tone(1); silence(1) }
                '-' -> { tone(3); silence(1) }
                ' ' -> silence(2)  // 1 unit already after last symbol -> 3 total
                '/' -> silence(6)  // -> 7 total
            }
        }
        return samples.toShortArray()
    }
}
