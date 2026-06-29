package com.pictofly.repository

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.StateFlow

interface AudioRepository {
    val currentVolumeDb: StateFlow<Int>

    fun initialize(context: Context, onInit: ((Boolean) -> Unit)? = null)
    fun setVolumeFromDb(dbValue: Int)
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH, utteranceId: String = "")
    fun stop()
    fun shutdown()
    fun isSpeaking(): Boolean
    fun setOnUtteranceProgressListener(listener: UtteranceProgressListener)
    fun adjustAndLockVolume(dbValue: Int)
    fun getCurrentVolumeInDb(): Int
    fun isMuted(): Boolean
    fun setMaxVolume()  // 👈 NUEVO MÉTODO
}