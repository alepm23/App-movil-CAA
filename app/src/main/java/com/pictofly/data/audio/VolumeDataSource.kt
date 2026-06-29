package com.pictofly.data.audio

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log

interface VolumeDataSource {
    fun initialize(context: Context)
    fun setVolumeFromDb(dbValue: Int)
    fun getCurrentVolumeInDb(): Int
    fun adjustAndLockVolume(dbValue: Int)
    fun isMuted(): Boolean
    fun getMaxVolume(): Int
    fun setMaxVolume()
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

        // Mapeo más agresivo: con 60 dB ya tienes ~80% del volumen máximo
        val volumeLevel = when {
            dbValue <= 0 -> 0
            dbValue >= 120 -> maxVolume
            else -> {
                // dB 60 = 80% del máximo, dB 100 = 95% del máximo
                val percent = (dbValue / 120f).coerceIn(0f, 1f)
                val adjustedPercent = (percent * 1.3f).coerceIn(0f, 1f)
                (adjustedPercent * maxVolume).toInt()
            }
        }

        Log.d("VolumeDataSource", "Máximo volumen sistema: $maxVolume")
        Log.d("VolumeDataSource", "dB: $dbValue -> Nivel: $volumeLevel")

        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volumeLevel,
            AudioManager.FLAG_SHOW_UI
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
    }

    override fun isMuted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Para Android M (API 23) en adelante
            !audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        } else {
            // Para versiones anteriores
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
        }
    }

    override fun getMaxVolume(): Int {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    override fun setMaxVolume() {
        val maxVolume = getMaxVolume()
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            maxVolume,
            AudioManager.FLAG_SHOW_UI
        )
        Log.d("VolumeDataSource", "Volumen forzado al máximo: $maxVolume")
    }
}