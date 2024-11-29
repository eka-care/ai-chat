package com.eka.conversation.features.audio

import com.eka.conversation.common.Response
import java.io.File

interface AudioProcessor {
    fun processAudio(audioFile: File?, onResult: (Response<String>) -> Unit)
}