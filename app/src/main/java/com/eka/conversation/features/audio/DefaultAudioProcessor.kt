package com.eka.conversation.features.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.SpeechRecognizer.ERROR_NETWORK
import android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT
import android.util.Log
import com.eka.conversation.common.PermissionUtils
import com.eka.conversation.common.Response
import java.io.File
import java.util.Locale

class DefaultAudioProcessor(
    private val context: Context
) : AudioProcessor {

    private var speechRecognizer: SpeechRecognizer? = null

    override fun processAudio(audioFile: File?, onResult: (Response<String>) -> Unit) {
        // If record audio permission not granted return error
        if (!PermissionUtils.hasRecordAudioPermission(context)) {
            onResult(Response.Error("Record Audio Permission Not Granted!"))
            return
        } else {
            startRecordingAudioAndTranscribe(onResult = onResult)
        }
    }

    private fun startRecordingAudioAndTranscribe(
        onResult: (Response<String>) -> Unit
    ) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onResult(Response.Error("Google Speech recognition not available on this device"))
            return
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                when (error) {
                    ERROR_NETWORK -> {
                        onResult(Response.Error("Network Error"))
                        Log.e("SpeechRecognizer", "Network error")
                    }

                    ERROR_SPEECH_TIMEOUT -> {
                        onResult(Response.Error("Speech Timeout"))
                        Log.e("SpeechRecognizer", "Speech timeout")
                    }

                    else -> {
                        onResult(Response.Error("Unknown Error with Error code: $error"))
                        Log.e("SpeechRecognizer", "Error code: $error")
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                results?.let {
                    val matches = it.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && matches.isNotEmpty()) {
                        val recognizedText = matches[0]
                        onResult(Response.Success(recognizedText))
                        Log.d("SpeechRecognizer", "Recognized Text: $recognizedText")
                    }
                }
                speechRecognizer?.startListening(intent)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.let {
                    val partialMatches = it.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (partialMatches != null && partialMatches.isNotEmpty()) {
                        val partialText = partialMatches[0]
                        onResult(Response.Success(partialText))
                        Log.d("SpeechRecognizer", "Partial Text: $partialText")
                    }
                }
                speechRecognizer?.startListening(intent)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // Start listening to the audio
        speechRecognizer?.startListening(intent)
    }

    // Stop the recording and transcription when done
    fun stopRecordingAndTranscribing() {
        speechRecognizer?.let {
            it.stopListening()
            it.destroy()
        }
        speechRecognizer = null
    }
}