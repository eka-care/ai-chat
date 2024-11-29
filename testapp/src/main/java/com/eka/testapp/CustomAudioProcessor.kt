package com.eka.testapp

import com.eka.conversation.common.Response
import com.eka.conversation.features.audio.AudioProcessor
import java.io.File

class CustomAudioProcessor : AudioProcessor {
    override fun processAudio(audioFile: File?, onResult: (Response<String>) -> Unit) {

    }
}