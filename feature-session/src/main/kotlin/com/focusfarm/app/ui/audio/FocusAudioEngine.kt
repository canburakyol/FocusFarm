package com.focusfarm.app.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.focusfarm.app.domain.AmbienceSound
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Random

class FocusAudioEngine(context: Context) {
    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null
    private var currentSound = AmbienceSound.NONE

    @Synchronized
    fun playAmbience(soundType: AmbienceSound) {
        if (soundType == AmbienceSound.NONE) {
            pauseAmbience()
            return
        }
        if (currentSound == soundType && player != null) {
            player?.start()
            return
        }

        releasePlayer()
        val source = ensureGeneratedAudio(soundType)
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            setDataSource(source.absolutePath)
            isLooping = true
            setVolume(AMBIENCE_VOLUME, AMBIENCE_VOLUME)
            setOnErrorListener { mediaPlayer, _, _ ->
                mediaPlayer.reset()
                true
            }
            prepare()
            start()
        }
        currentSound = soundType
    }

    @Synchronized
    fun pauseAmbience() {
        player?.takeIf { it.isPlaying }?.pause()
    }

    @Synchronized
    fun stopAndRelease() {
        releasePlayer()
        currentSound = AmbienceSound.NONE
    }

    private fun releasePlayer() {
        player?.runCatching {
            if (isPlaying) stop()
            reset()
            release()
        }
        player = null
    }

    private fun ensureGeneratedAudio(sound: AmbienceSound): File {
        val file = File(appContext.cacheDir, "focus_${sound.name.lowercase()}_v1.wav")
        if (file.exists() && file.length() > WAV_HEADER_SIZE) return file

        val sampleCount = SAMPLE_RATE * LOOP_SECONDS
        val random = Random(sound.ordinal.toLong() + 41L)
        BufferedOutputStream(FileOutputStream(file)).use { output ->
            writeWavHeader(output, sampleCount)
            var rainFilter = 0.0
            repeat(sampleCount) {
                val noise = random.nextDouble() * 2.0 - 1.0
                val sample = when (sound) {
                    AmbienceSound.RAIN -> {
                        rainFilter = rainFilter * 0.92 + noise * 0.08
                        rainFilter * 0.72
                    }
                    AmbienceSound.WHITE_NOISE -> noise * 0.32
                    AmbienceSound.NONE -> 0.0
                }
                val pcm = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt()
                output.write(pcm and 0xff)
                output.write((pcm shr 8) and 0xff)
            }
        }
        return file
    }

    private fun writeWavHeader(output: BufferedOutputStream, sampleCount: Int) {
        val dataSize = sampleCount * BYTES_PER_SAMPLE
        output.write("RIFF".toByteArray())
        writeInt(output, 36 + dataSize)
        output.write("WAVEfmt ".toByteArray())
        writeInt(output, 16)
        writeShort(output, 1)
        writeShort(output, 1)
        writeInt(output, SAMPLE_RATE)
        writeInt(output, SAMPLE_RATE * BYTES_PER_SAMPLE)
        writeShort(output, BYTES_PER_SAMPLE)
        writeShort(output, 16)
        output.write("data".toByteArray())
        writeInt(output, dataSize)
    }

    private fun writeInt(output: BufferedOutputStream, value: Int) {
        repeat(4) { output.write((value shr (it * 8)) and 0xff) }
    }

    private fun writeShort(output: BufferedOutputStream, value: Int) {
        repeat(2) { output.write((value shr (it * 8)) and 0xff) }
    }

    private companion object {
        const val AMBIENCE_VOLUME = 0.3f
        const val SAMPLE_RATE = 22_050
        const val LOOP_SECONDS = 8
        const val BYTES_PER_SAMPLE = 2
        const val WAV_HEADER_SIZE = 44L
    }
}

