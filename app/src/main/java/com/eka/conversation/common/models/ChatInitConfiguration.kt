package com.eka.conversation.common.models

import androidx.annotation.Keep
import com.eka.conversation.client.Environment
import com.eka.networking.client.NetworkConfig

@Keep
data class ChatInitConfiguration(
    val audioFeatureConfiguration: AudioFeatureConfiguration = AudioFeatureConfiguration(),
    val networkConfig: NetworkConfig,
    val environment: Environment = Environment.PROD,
    val authConfiguration: AuthConfiguration
)
