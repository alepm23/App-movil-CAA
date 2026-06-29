package com.pictofly.repository

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.pictofly.data.audio.TTSDataSource
import com.pictofly.data.audio.VolumeDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepositoryImpl @Inject constructor(
    private val ttsDataSource: TTSDataSource,
    private val volumeDataSource: VolumeDataSource
) : AudioRepository {

    private val _currentVolumeDb = MutableStateFlow(70)
    override val currentVolumeDb: StateFlow<Int> = _currentVolumeDb

    private var isInitialized = false

    override fun initialize(context: Context, onInit: ((Boolean) -> Unit)?) {
        if (isInitialized) {
            onInit?.invoke(true)
            return
        }

        volumeDataSource.initialize(context)
        _currentVolumeDb.value = volumeDataSource.getCurrentVolumeInDb()

        ttsDataSource.initialize(context) { success ->
            isInitialized = success
            if (success) {
                ttsDataSource.speak("Audio listo", TextToSpeech.QUEUE_FLUSH, "init")
            }
            onInit?.invoke(success)
        }
    }

    override fun setVolumeFromDb(dbValue: Int) {
        ttsDataSource.setVolumeFromDb(dbValue)
        _currentVolumeDb.value = dbValue
    }

    override fun speak(text: String, queueMode: Int, utteranceId: String) {
        if (!isInitialized) {
            Log.e("AudioRepository", "TTS no inicializado, no se puede hablar: $text")
            return
        }
        ttsDataSource.speak(text, queueMode, utteranceId)
    }

    override fun stop() {
        ttsDataSource.stop()
    }

    override fun shutdown() {
        ttsDataSource.shutdown()
        isInitialized = false
    }

    override fun isSpeaking(): Boolean {
        return ttsDataSource.isSpeaking()
    }

    override fun setOnUtteranceProgressListener(listener: UtteranceProgressListener) {
        ttsDataSource.setOnUtteranceProgressListener(listener)
    }

    override fun adjustAndLockVolume(dbValue: Int) {
        volumeDataSource.adjustAndLockVolume(dbValue)
        _currentVolumeDb.value = dbValue
    }

    override fun getCurrentVolumeInDb(): Int {
        return volumeDataSource.getCurrentVolumeInDb()
    }

    override fun isMuted(): Boolean {
        return volumeDataSource.isMuted()
    }

    override fun setMaxVolume() {  // 👈 NUEVA IMPLEMENTACIÓN
        volumeDataSource.setMaxVolume()
    }
}