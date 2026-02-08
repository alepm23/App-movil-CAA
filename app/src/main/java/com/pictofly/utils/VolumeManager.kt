package com.pictofly.utils

import android.content.Context
import android.media.AudioManager
import android.util.Log

object VolumeManager {
    private var audioManager: AudioManager? = null

    fun initialize(context: Context) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Log.d("VolumeManager", "VolumeManager inicializado")
    }

    fun getCurrentVolume(): Int {
        return audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
    }

    fun getMaxVolume(): Int {
        return audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
    }

    fun setVolume(level: Int) {
        audioManager?.let {
            val maxVolume = getMaxVolume()
            val adjustedLevel = level.coerceIn(0, maxVolume)
            it.setStreamVolume(AudioManager.STREAM_MUSIC, adjustedLevel, 0)
            Log.d("VolumeManager", "Volumen establecido en $adjustedLevel/$maxVolume")
        }
    }

    fun increaseVolume() {
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            0
        )
    }

    fun decreaseVolume() {
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            0
        )
    }
}