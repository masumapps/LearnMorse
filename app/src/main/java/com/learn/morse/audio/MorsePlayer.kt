package com.learn.morse.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import com.learn.morse.monetization.Entitlements
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** A stretch of the signal: on (a dot or dash) or off (a gap), lasting [ms]. */
data class Segment(val on: Boolean, val ms: Long)

/**
 * Plays morse made of '.', '-', spaces between letters and '/' between words, through every output
 * the user turned on: a sine tone, vibration, a screen flash and the flashlight. All of them follow
 * the same [segments], so they stay in step. Cancelling the calling coroutine stops playback.
 */
object MorsePlayer {
    private const val SAMPLE_RATE = 44100
    private const val RAMP_MS = 5

    // True while the signal is on and the screen flash is enabled; MainActivity draws the flash.
    private val _flashOn = MutableStateFlow(false)
    val flashOn: StateFlow<Boolean> = _flashOn.asStateFlow()

    suspend fun play(context: Context, morse: String, settings: SoundSettings = SoundSettings.load(context)) {
        val segments = segments(morse, settings.unitMs, settings.gapUnitMs)
        if (segments.none { it.on }) return
        val torch = settings.flashlight && Entitlements.isPremium.value
        coroutineScope {
            if (settings.sound) launch { playTone(segments, settings.frequency.toDouble(), settings.volume) }
            if (settings.vibrate) launch { Haptics.playPattern(context, segments) }
            if (settings.screenFlash || torch) launch { blink(context, segments, settings.screenFlash, torch) }
        }
    }

    /**
     * The signal as on/off stretches. Gaps inside a letter are one [unitMs]; between letters 3 and
     * between words 7 [gapUnitMs], which only differs from [unitMs] with Farnsworth spacing.
     */
    fun segments(morse: String, unitMs: Int, gapUnitMs: Int): List<Segment> {
        val segments = mutableListOf<Segment>()
        // 0: next symbol is in the same letter, 1: a new letter, 2: a new word.
        var gap = 0
        for (symbol in morse) {
            when (symbol) {
                '.', '-' -> {
                    if (segments.isNotEmpty()) {
                        val ms = when (gap) {
                            2 -> 7L * gapUnitMs
                            1 -> 3L * gapUnitMs
                            else -> unitMs.toLong()
                        }
                        segments += Segment(on = false, ms = ms)
                    }
                    segments += Segment(on = true, ms = unitMs * if (symbol == '-') 3L else 1L)
                    gap = 0
                }
                '/' -> gap = 2
                ' ' -> gap = maxOf(gap, 1)
            }
        }
        // Trailing silence, so the last symbol isn't cut off by the end of playback.
        if (segments.isNotEmpty()) segments += Segment(on = false, ms = unitMs.toLong())
        return segments
    }

    private suspend fun blink(context: Context, segments: List<Segment>, screen: Boolean, torch: Boolean) {
        try {
            for (segment in segments) {
                if (screen) _flashOn.value = segment.on
                if (torch) Torch.set(context, segment.on)
                delay(segment.ms)
            }
        } finally {
            _flashOn.value = false
            if (torch) Torch.set(context, false)
        }
    }

    private suspend fun playTone(segments: List<Segment>, frequency: Double, volume: Float) {
        val pcm = withContext(Dispatchers.Default) { buildPcm(segments, frequency, volume) }
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

    private fun buildPcm(segments: List<Segment>, frequency: Double, volume: Float): ShortArray {
        val pcm = ShortArray(segments.sumOf { (SAMPLE_RATE * it.ms / 1000).toInt() })
        var offset = 0
        for (segment in segments) {
            val length = (SAMPLE_RATE * segment.ms / 1000).toInt()
            if (segment.on) {
                val ramp = min(SAMPLE_RATE * RAMP_MS / 1000, length / 2)
                for (i in 0 until length) {
                    // Short fade in/out avoids audible clicks at tone edges.
                    val envelope = when {
                        i < ramp -> i.toDouble() / ramp
                        i > length - ramp -> (length - i).toDouble() / ramp
                        else -> 1.0
                    }
                    pcm[offset + i] = (sin(2 * PI * frequency * i / SAMPLE_RATE) * envelope * Short.MAX_VALUE * volume).toInt().toShort()
                }
            }
            // Silence is already zero.
            offset += length
        }
        return pcm
    }
}
