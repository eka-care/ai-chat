package com.eka.conversation.features.audio

interface ISpeechToText {
    fun onSpeechToTextComplete(result: Result<String?>)
}