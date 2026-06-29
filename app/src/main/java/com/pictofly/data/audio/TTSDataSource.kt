package com.pictofly.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.pictofly.data.model.TTSConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface TTSDataSource {
    fun initialize(context: Context, onInit: ((Boolean) -> Unit)? = null)
    fun setVolumeFromDb(dbValue: Int)
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH, utteranceId: String = "")
    fun stop()
    fun shutdown()
    fun isSpeaking(): Boolean
    fun setOnUtteranceProgressListener(listener: UtteranceProgressListener)
}

@Singleton
class TTSDataSourceImpl @Inject constructor() : TTSDataSource {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentConfig = TTSConfig()

    override fun initialize(context: Context, onInit: ((Boolean) -> Unit)?) {
        if (textToSpeech != null) {
            onInit?.invoke(isInitialized)
            return
        }

        textToSpeech = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS //guardamos si fue exitosa
            if (isInitialized) {
                val result = textToSpeech?.setLanguage(Locale("es", "ES"))

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Idioma español no soportado, usando idioma por defecto")
                    textToSpeech?.language = Locale.getDefault()
                } else {
                    Log.d("TTS", "TTS inicializado con español")
                }

                textToSpeech?.setSpeechRate(currentConfig.speed)
                textToSpeech?.setPitch(currentConfig.pitch)
            } else {
                Log.e("TTS", "Error inicializando TTS")
            }
            onInit?.invoke(isInitialized)
        }
    }

    override fun setVolumeFromDb(dbValue: Int) {
        if (!isInitialized) return
        currentConfig = currentConfig.copy(volumeDb = dbValue)
    }

    override fun speak(text: String, queueMode: Int, utteranceId: String) {
        if (!isInitialized) {
            Log.e("TTS", "TTS no inicializado, no se puede hablar: $text")
            return
        }
        if (text.isEmpty()) return

        Log.d("TTS", "Hablando: $text")

        scope.launch {
            textToSpeech?.speak(
                text,
                queueMode,
                null,
                utteranceId.ifEmpty { System.currentTimeMillis().toString() }
            )
        }
    }

    override fun stop() {
        textToSpeech?.stop()
    }

    override fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }

    override fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }

    override fun setOnUtteranceProgressListener(listener: UtteranceProgressListener) {
        textToSpeech?.setOnUtteranceProgressListener(listener)
    }
}