package com.eka.conversation.common.models

import androidx.annotation.Keep

@Keep
data class UserInfo(
    val userId: String,
    val businessId: String
)
