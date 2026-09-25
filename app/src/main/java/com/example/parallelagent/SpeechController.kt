package com.example.parallelagent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechController(
    private val context: Context,
    private val onStatus: (String) -> Unit,
    private val onResult: (String) -> Unit
) {

    private var recognizer: SpeechRecognizer? = null

    fun startListening() {

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onStatus("⚠ Speech recognition unavailable")
            return
        }

        recognizer?.destroy()

        recognizer = SpeechRecognizer.createSpeechRecognizer(context)

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    onStatus("🎙 Listening...")
                }

                override fun onBeginningOfSpeech() {
                    onStatus("🎙 Listening...")
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    onStatus("◌ Understanding...")
                }

                override fun onError(error: Int) {
                    onStatus("⚠ Didn't catch that")
                }

                override fun onResults(results: Bundle?) {

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val text = matches?.firstOrNull()

                    if (text != null) {
                        onResult(text)
                    } else {
                        onStatus("⚠ No speech detected")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            }
        )

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "zh-CN"
            )

            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
            )
        }

        recognizer?.startListening(intent)
    }

    fun stop() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }
}