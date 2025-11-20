package com.eka.conversation.common.models

import androidx.annotation.Keep

@Keep
data class AuthConfiguration(
    val agentId: String,
    val userId: String,
    val businessId: String
)
