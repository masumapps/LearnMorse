package com.learn.morse.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * A tone that sounds for as long as a key is held, like a real morse key.
 * Call [start] on press, [stop] on release and [close] when done.
 */
class KeyTone(private val frequency: Double = 800.0, private val volume: Float = 0.8f) {
    private val sampleRate = 44100
    private val chunkSize = 256
    // Short fade in/out avoids clicks when the key goes down or up.
    private val rampStep = 1.0 / (sampleRate * 5 / 1000)

    private val track: AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(
            AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        )
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }
        }
        .build()

    @Volatile private var keyDown = false
    @Volatile private var closed = false
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null

    fun start() {
        if (closed) return
        keyDown = true
        if (running.compareAndSet(false, true)) {
            worker = thread(name = "KeyTone") {
                // Restart if the key went down again just as the previous tone finished fading out.
                do {
                    stream()
                    running.set(false)
                } while (keyDown && !closed && running.compareAndSet(false, true))
            }
        }
    }

    fun stop() {
        keyDown = false
    }

    fun close() {
        closed = true
        keyDown = false
        worker?.join(200)
        track.release()
    }

    private fun stream() {
        val chunk = ShortArray(chunkSize)
        val phaseStep = 2 * PI * frequency / sampleRate
        var phase = 0.0
        var gain = 0.0
        track.play()
        while (!closed && (keyDown || gain > 0.0)) {
            for (i in chunk.indices) {
                gain = if (keyDown) minOf(1.0, gain + rampStep) else maxOf(0.0, gain - rampStep)
                chunk[i] = (sin(phase) * gain * Short.MAX_VALUE * volume).toInt().toShort()
                phase = (phase + phaseStep) % (2 * PI)
            }
            track.write(chunk, 0, chunk.size)
        }
        if (!closed) {
            // Push the fade-out through the buffer with silence before pausing. Flushing right away
            // threw away the tail of the tone, cutting it off abruptly and making an extra "pop".
            track.write(ShortArray(track.bufferSizeInFrames), 0, track.bufferSizeInFrames)
            track.pause()
            track.flush()
        }
    }
}
