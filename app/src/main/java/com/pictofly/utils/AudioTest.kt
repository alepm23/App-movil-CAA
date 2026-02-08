package com.pictofly.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

object AudioTest {
    private var tts: TextToSpeech? = null
    private var isReady = false

    fun testTTS(context: Context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("es", "ES"))
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isReady = true
                    tts?.speak("Prueba de audio funcionando correctamente", TextToSpeech.QUEUE_FLUSH, null, "test")
                } else {
                    Log.e("AudioTest", "Idioma no soportado")
                }
            } else {
                Log.e("AudioTest", "Error inicializando TTS")
            }
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}