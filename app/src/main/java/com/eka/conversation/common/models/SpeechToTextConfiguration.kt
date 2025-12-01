package com.eka.conversation.common.models

import androidx.annotation.Keep
import com.eka.conversation.features.audio.ISpeechToText

@Keep
data class SpeechToTextConfiguration(
    val speechToText: ISpeechToText? = null,
)