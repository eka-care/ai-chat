package com.eka.conversation.common.models

import androidx.annotation.Keep
import com.eka.conversation.client.Environment

@Keep
data class ChatInitConfiguration(
    val audioFeatureConfiguration: AudioFeatureConfiguration = AudioFeatureConfiguration(),
    val environment: Environment = Environment.PROD,
    val authConfiguration: AuthConfiguration
)
