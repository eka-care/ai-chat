package com.eka.conversation.common.models

import androidx.annotation.Keep
import com.eka.conversation.features.audio.AudioProcessor

@Keep
data class AudioFeatureConfiguration(
    val isEnabled: Boolean = true,
    val audioProcessorType: AudioProcessorType = AudioProcessorType.GOOGLE_SPEECH_RECOGNIZER,
    val audioProcessor: AudioProcessor? = null,
)

enum class AudioProcessorType {
    GOOGLE_SPEECH_RECOGNIZER, // Default Speech recognizer
    CUSTOM, // Implemented by user
    VOSK //
}