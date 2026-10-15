package com.example.godsmino.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Gestor de efectos de sonido nativos sintéticos para GODSMINO.
 * Genera tonos y efectos acústicos (clack de fichas, poderes, victorias) en tiempo real
 * sin necesidad de archivos multimedia externos.
 */
object SoundManager {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playTileClack() {
        scope.launch {
            generateTone(freqHz = 800.0, durationMs = 40, volume = 0.8f)
            generateTone(freqHz = 400.0, durationMs = 30, volume = 0.5f)
        }
    }

    fun playPowerSound() {
        scope.launch {
            generateTone(freqHz = 523.25, durationMs = 80, volume = 0.7f) // C5
            generateTone(freqHz = 659.25, durationMs = 80, volume = 0.7f) // E5
            generateTone(freqHz = 783.99, durationMs = 120, volume = 0.8f) // G5
        }
    }

    fun playDrawTile() {
        scope.launch {
            generateTone(freqHz = 300.0, durationMs = 50, volume = 0.4f)
        }
    }

    fun playVictorySound() {
        scope.launch {
            val notes = listOf(523.25, 659.25, 783.99, 1046.50)
            for (note in notes) {
                generateTone(freqHz = note, durationMs = 100, volume = 0.9f)
            }
        }
    }

    fun playClickSound() {
        scope.launch {
            generateTone(freqHz = 1200.0, durationMs = 20, volume = 0.3f)
        }
    }

    private fun generateTone(freqHz: Double, durationMs: Int, volume: Float) {
        try {
            val sampleRate = 22050
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val sample = ByteArray(2 * numSamples)

            for (i in 0 until numSamples) {
                val angle = 2.0 * Math.PI * i / (sampleRate / freqHz)
                val valSample = (sin(angle) * 32767 * volume).toInt().toShort()
                sample[2 * i] = (valSample.toInt() and 0x00ff).toByte()
                sample[2 * i + 1] = (valSample.toInt() shr 8 and 0x00ff).toByte()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(sample.size)
                .build()

            audioTrack.write(sample, 0, sample.size)
            audioTrack.play()
            audioTrack.release()
        } catch (_: Exception) {
            // Safe fallback if audio hardware is busy
        }
    }
}
