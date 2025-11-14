package com.eka.conversation.common.models

data class ChatInitConfiguration(
    val audioFeatureConfiguration: AudioFeatureConfiguration = AudioFeatureConfiguration(),
    val authConfiguration: AuthConfiguration
)
