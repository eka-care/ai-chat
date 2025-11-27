package com.eka.conversation.features.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.eka.conversation.common.PermissionChecker
import java.io.File
import java.io.FileOutputStream

class AndroidAudioRecorder(
    private val context: Context
) : AudioRecorder {

    private var recorder: MediaRecorder? = null

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    override fun startRecording(outputFile: File, onError: (String) -> Unit) {
        if (!PermissionChecker.hasRecordAudioPermission(context)) {
            onError("Record Audio Permission Not Granted!")
            return
        }
        try {
            createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(FileOutputStream(outputFile).fd)

                prepare()
                start()

                recorder = this
            }
        } catch (e: Exception) {
            onError("Something went wrong!")
        }
    }

    override fun stopRecording() {
        recorder?.let {
            it.stop()
            it.reset()
        }
        recorder = null
    }
}