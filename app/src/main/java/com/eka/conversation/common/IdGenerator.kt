package com.eka.conversation.common

import java.util.UUID

object IdGenerator {
    fun generateSessionId(): String {
        return UUID.randomUUID().toString() + "_" + TimeUtils.getCurrentUTCEpochMillis()
    }
}
