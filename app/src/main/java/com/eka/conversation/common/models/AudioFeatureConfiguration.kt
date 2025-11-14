package com.eka.conversation.common.models

import androidx.annotation.Keep
import com.eka.conversation.features.audio.AudioProcessor

@Keep
data class AudioFeatureConfiguration(
    val isEnabled: Boolean = true,
    val audioProcessorType: AudioProcessorType = AudioProcessorType.EKA,
    val audioProcessor: AudioProcessor? = null,
)

enum class AudioProcessorType {
    GOOGLE_SPEECH_RECOGNIZER, // Default Speech recognizer
    EKA,
    CUSTOM, // Implemented by user
}