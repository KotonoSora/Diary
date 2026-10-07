package com.kotonosora.todolist.feature.flashcard

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import java.util.Locale

@Composable
fun rememberFlashcardSpeaker(): (String) -> Unit {
    if (LocalInspectionMode.current) return { _: String -> }
    val context = LocalContext.current
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val e = tts ?: return@TextToSpeech
                val result = e.setLanguage(Locale.US)
                ready = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                e.setSpeechRate(0.9f)
            }
        }
        engine = tts
        onDispose {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (_: Exception) {
            }
            engine = null
        }
    }

    return remember(engine, ready) {
        { text: String ->
            val e = engine
            if (ready && e != null && text.isNotBlank()) {
                try {
                    e.speak(
                        text,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "flashcard_${System.currentTimeMillis()}"
                    )
                } catch (_: Exception) {
                }
            }
        }
    }
}
