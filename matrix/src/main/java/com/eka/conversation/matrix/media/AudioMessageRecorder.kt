package com.eka.conversation.matrix.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import java.io.File

/** One finished voice note, base64 encoded the way Matrix expects (`ct: audio`). */
data class AudioPayload(
    val audio: String,
    val format: String,
    val durationMs: Long,
    val timestamp: Long,
)

/**
 * Port of synapse `AudioManager`: record one clip, hand back base64 on stop.
 * Uses WebM/Opus (same mime the web widget sends) where the OS supports it, AAC/MP4 otherwise.
 */
class AudioMessageRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L
    private var format = ""
    private var onDone: ((AudioPayload) -> Unit)? = null
    private var onError: ((Throwable) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stop() }

    val isRecording get() = recorder != null

    @Suppress("DEPRECATION")
    fun start(onDone: (AudioPayload) -> Unit, onError: (Throwable) -> Unit) {
        if (recorder != null) return
        this.onDone = onDone
        this.onError = onError
        try {
            // Matrix rejects Android's WebM/Opus ("invalid_content: Invalid audio format");
            // AAC in MP4 is accepted and transcribed in ~2 s, so always record that.
            val useWebm = false
            val ext = "m4a"
            format = "audio/mp4"
            val out = File(context.cacheDir, "medassist_${System.currentTimeMillis()}.$ext")
            file = out
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            if (useWebm) {
                r.setOutputFormat(MediaRecorder.OutputFormat.WEBM)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
            } else {
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            }
            r.setAudioChannels(1)
            r.setAudioSamplingRate(16000)
            r.setAudioEncodingBitRate(32000)
            r.setOutputFile(out.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            startedAt = System.currentTimeMillis()
            handler.postDelayed(autoStop, MAX_DURATION_MS)
        } catch (e: Throwable) {
            release()
            onError(e)
        }
    }

    fun stop() {
        handler.removeCallbacks(autoStop)
        val r = recorder ?: return
        val f = file
        try {
            r.stop()
            release()
            val bytes = f?.readBytes() ?: ByteArray(0)
            f?.delete()
            if (bytes.isEmpty()) throw IllegalStateException("Empty recording")
            onDone?.invoke(
                AudioPayload(
                    audio = Base64.encodeToString(bytes, Base64.NO_WRAP),
                    format = format,
                    durationMs = System.currentTimeMillis() - startedAt,
                    timestamp = System.currentTimeMillis(),
                )
            )
        } catch (e: Throwable) {
            release()
            f?.delete()
            onError?.invoke(e)
        }
    }

    /** Discard without sending. */
    fun cancel() {
        handler.removeCallbacks(autoStop)
        try {
            recorder?.stop()
        } catch (_: Throwable) {
        }
        release()
        file?.delete()
    }

    private fun release() {
        try {
            recorder?.release()
        } catch (_: Throwable) {
        }
        recorder = null
    }

    companion object {
        const val MAX_DURATION_MS = 15 * 60 * 1000L
    }
}
