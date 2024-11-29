package com.eka.conversation.features.audio

import java.io.File

interface AudioRecorder {
    fun startRecording(outputFile: File, onError: (String) -> Unit)
    fun stopRecording()
}