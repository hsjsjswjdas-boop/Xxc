package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

class GameAudioEngine {
    private val sampleRate = 22050
    private val scope = CoroutineScope(Dispatchers.Default)
    var isMuted = false

    // Pre-generated sound byte buffers for instant latency-free playback
    private val rifleSound: ShortArray = generateRifleShot()
    private val shotgunSound: ShortArray = generateShotgunShot()
    private val sniperSound: ShortArray = generateSniperShot()
    private val katanaSound: ShortArray = generateKatanaSlash()
    private val explosionSound: ShortArray = generateExplosion()
    private val hitSound: ShortArray = generateHitImpact()
    private val hurtSound: ShortArray = generateHurt()
    private val lootSound: ShortArray = generateLootPickup()
    private val healSound: ShortArray = generateHeal()
    private val sirenSound: ShortArray = generateSiren()
    private val victorySound: ShortArray = generateVictoryFanfare()
    private val defeatSound: ShortArray = generateDefeatTone()

    private fun playPcm(pcm: ShortArray, volume: Float = 1.0f) {
        if (isMuted) return
        scope.launch {
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = max(minBufferSize, pcm.size * 2)

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
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
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.setVolume(volume.coerceIn(0f, 1f))
                audioTrack.write(pcm, 0, pcm.size)
                audioTrack.play()

                // Release after playback completes
                val durationMs = (pcm.size.toDouble() / sampleRate * 1000).toLong() + 50
                kotlinx.coroutines.delay(durationMs)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // Ignore transient audio error
            }
        }
    }

    fun playRifle() = playPcm(rifleSound, 0.75f)
    fun playShotgun() = playPcm(shotgunSound, 0.9f)
    fun playSniper() = playPcm(sniperSound, 0.95f)
    fun playKatana() = playPcm(katanaSound, 0.7f)
    fun playExplosion() = playPcm(explosionSound, 1.0f)
    fun playHit() = playPcm(hitSound, 0.7f)
    fun playPlayerHurt() = playPcm(hurtSound, 0.85f)
    fun playLoot() = playPcm(lootSound, 0.8f)
    fun playHeal() = playPcm(healSound, 0.85f)
    fun playZoneWarning() = playPcm(sirenSound, 0.7f)
    fun playVictory() = playPcm(victorySound, 0.95f)
    fun playDefeat() = playPcm(defeatSound, 0.9f)

    // Sound Synthesis Algorithms
    private fun generateRifleShot(): ShortArray {
        val duration = 0.18f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1f - t / duration).pow(3f)
            val noise = (Random.nextFloat() * 2f - 1f)
            val tone = sin(2 * PI * 180 * (1f - t * 4) * t).toFloat()
            val sample = ((noise * 0.7f + tone * 0.3f) * decay * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateShotgunShot(): ShortArray {
        val duration = 0.32f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1f - t / duration).pow(2.2f)
            val noise = (Random.nextFloat() * 2f - 1f)
            val rumble = sin(2 * PI * 65 * t).toFloat()
            val sample = ((noise * 0.8f + rumble * 0.5f) * decay * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateSniperShot(): ShortArray {
        val duration = 0.45f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1f - t / duration).pow(1.8f)
            val snap = if (t < 0.04f) (Random.nextFloat() * 2f - 1f) * 1.5f else (Random.nextFloat() * 2f - 1f) * 0.4f
            val punch = sin(2 * PI * 220 * exp(-t * 12) * t).toFloat()
            val sample = ((snap * 0.7f + punch * 0.6f) * decay * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateKatanaSlash(): ShortArray {
        val duration = 0.2f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = sin(t / duration * PI.toFloat())
            val freq = 400f + 1600f * (1f - t / duration)
            val swoosh = (Random.nextFloat() * 0.4f + 0.6f * sin(2 * PI * freq * t).toFloat())
            val sample = (swoosh * decay * Short.MAX_VALUE * 0.6f).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateExplosion(): ShortArray {
        val duration = 1.1f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1f - t / duration).pow(2f)
            val noise = (Random.nextFloat() * 2f - 1f)
            val sub = sin(2 * PI * (45f + 25f * (1f - t)) * t).toFloat()
            val sample = ((noise * 0.65f + sub * 0.75f) * decay * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateHitImpact(): ShortArray {
        val duration = 0.08f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1f - t / duration).pow(3f)
            val thud = sin(2 * PI * 130 * t).toFloat() + (Random.nextFloat() - 0.5f) * 0.3f
            val sample = (thud * decay * Short.MAX_VALUE * 0.8f).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateHurt(): ShortArray {
        val duration = 0.22f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1f - t / duration).pow(2f)
            val groan = sin(2 * PI * (90 - t * 40) * t).toFloat()
            val sample = (groan * decay * Short.MAX_VALUE * 0.75f).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateLootPickup(): ShortArray {
        val duration = 0.28f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        val f1 = 587.33f // D5
        val f2 = 880f    // A5
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val freq = if (t < 0.12f) f1 else f2
            val decay = (1f - t / duration).pow(1.5f)
            val chime = sin(2 * PI * freq * t).toFloat() + 0.3f * sin(4 * PI * freq * t).toFloat()
            val sample = (chime * decay * Short.MAX_VALUE * 0.65f).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateHeal(): ShortArray {
        val duration = 0.45f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val freq = 440f + t * 440f
            val decay = sin(t / duration * PI.toFloat())
            val tone = sin(2 * PI * freq * t).toFloat() * 0.6f + sin(4 * PI * freq * t).toFloat() * 0.2f
            val sample = (tone * decay * Short.MAX_VALUE * 0.55f).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateSiren(): ShortArray {
        val duration = 0.6f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val freq = 350f + 180f * sin(2 * PI * 3 * t).toFloat()
            val decay = (1f - (t / duration - 0.5f).pow(2) * 2f).coerceAtLeast(0f)
            val tone = sin(2 * PI * freq * t).toFloat()
            val sample = (tone * decay * Short.MAX_VALUE * 0.5f).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateVictoryFanfare(): ShortArray {
        val duration = 1.4f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        // C5 (523), E5 (659), G5 (784), C6 (1046) arpeggio
        val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val noteIndex = (t / 0.3f).toInt().coerceIn(0, 3)
            val freq = notes[noteIndex]
            val tInNote = t % 0.3f
            val noteDecay = (1f - tInNote / 0.35f).coerceIn(0f, 1f)
            val brass = sin(2 * PI * freq * t).toFloat() * 0.5f +
                    sin(4 * PI * freq * t).toFloat() * 0.25f +
                    sin(6 * PI * freq * t).toFloat() * 0.12f
            val sample = (brass * noteDecay * Short.MAX_VALUE * 0.6f).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateDefeatTone(): ShortArray {
        val duration = 0.9f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val freq = 220f - 80f * (t / duration)
            val decay = (1f - t / duration).pow(1.5f)
            val tone = sin(2 * PI * freq * t).toFloat() + 0.3f * sin(4 * PI * freq * t).toFloat()
            val sample = (tone * decay * Short.MAX_VALUE * 0.6f).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }
}
