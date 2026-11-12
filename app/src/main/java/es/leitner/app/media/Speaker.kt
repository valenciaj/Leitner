package es.leitner.app.media

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Lectura en voz alta con el motor de texto a voz del móvil, en el idioma del sistema. */
class Speaker(context: Context) {
    var isReady by mutableStateOf(false)
        private set
    var isSpeaking by mutableStateOf(false)
        private set

    /** Texto pendiente si se pidió leer antes de que el motor estuviera listo. */
    private var pending: String? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            onReady()
        }
    }

    private fun onReady() {
        val result = tts.setLanguage(Locale.getDefault())
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale("es", "ES"))
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { isSpeaking = true }
            override fun onDone(utteranceId: String?) { isSpeaking = false }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { isSpeaking = false }
        })
        isReady = true
        pending?.let { speak(it) }
        pending = null
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!isReady) {
            pending = text
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "leitner")
    }

    fun stop() {
        pending = null
        if (isReady) tts.stop()
        isSpeaking = false
    }

    fun shutdown() {
        stop()
        tts.shutdown()
    }
}

@Composable
fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
    return speaker
}
