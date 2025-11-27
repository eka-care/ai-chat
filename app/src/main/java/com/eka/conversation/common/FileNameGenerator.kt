package com.eka.conversation.common

import com.eka.conversation.data.local.db.entities.models.MessageFileType

object FileNameGenerator {
    fun generateFileName(fileType: MessageFileType): String {
        return TimeUtils.getCurrentUTCEpochMillis().toString() + "_" + fileType.name
    }
}
