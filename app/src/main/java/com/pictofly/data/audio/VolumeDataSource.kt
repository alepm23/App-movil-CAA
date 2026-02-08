package com.pictofly.data.audio

import android.content.Context
import android.media.AudioManager
import android.os.Build

interface VolumeDataSource {
    fun initialize(context: Context)
    fun setVolumeFromDb(dbValue: Int)
    fun getCurrentVolumeInDb(): Int
    fun adjustAndLockVolume(dbValue: Int)
    fun isMuted(): Boolean
    fun getMaxVolume(): Int
}

class VolumeDataSourceImpl : VolumeDataSource {
    private lateinit var audioManager: AudioManager
    private lateinit var context: Context

    override fun initialize(context: Context) {
        this.context = context
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun setVolumeFromDb(dbValue: Int) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        val volumeLevel = when {
            dbValue <= 0 -> 0
            dbValue >= 120 -> maxVolume
            else -> ((dbValue / 120f) * maxVolume).toInt()
        }

        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volumeLevel,
            0
        )
    }

    override fun getCurrentVolumeInDb(): Int {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        if (maxVolume == 0) return 0

        return ((currentVolume.toFloat() / maxVolume.toFloat()) * 120).toInt()
    }

    override fun adjustAndLockVolume(dbValue: Int) {
        setVolumeFromDb(dbValue)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun isMuted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        } else {
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
        }
    }

    override fun getMaxVolume(): Int {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }
}