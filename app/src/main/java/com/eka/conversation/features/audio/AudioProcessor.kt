package com.eka.conversation.features.audio

interface ISpeechToText {
    fun onSpeechToTextComplete(text: String?)
}