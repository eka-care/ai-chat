package com.eka.conversation.matrix

import com.eka.conversation.matrix.connection.DisconnectionDetails
import kotlinx.serialization.json.JsonObject

/** Realtime events surfaced to the UI (mirrors SYNAPSE_REALTIME_EVENTS). */
sealed class MatrixEvent {
    object Connected : MatrixEvent()
    data class Disconnected(val details: DisconnectionDetails) : MatrixEvent()
    data class MessageChunk(val messageId: String, val text: String, val timestamp: Long?) : MatrixEvent()
    data class Progress(val messageId: String?, val text: String) : MatrixEvent()
    data class Tips(val tips: List<String>) : MatrixEvent()
    object EndOfStream : MatrixEvent()
    data class ToolCall(val messageId: String, val timestamp: Long?, val data: ToolCallData, val raw: JsonObject) : MatrixEvent()
    data class AudioTranscript(val text: String) : MatrixEvent()
    data class ToolStart(val messageId: String, val name: String?) : MatrixEvent()
    data class ToolEnd(val messageId: String, val name: String?) : MatrixEvent()
    data class RealtimeError(val code: String?, val message: String?) : MatrixEvent()
    /** Device lost its network; the transport was dropped so nothing is sent into a dead socket. */
    object Offline : MatrixEvent()
    /** Network is back; the client is reconnecting the current session. */
    object Online : MatrixEvent()
}

/** One outgoing chat request (mirrors synapse SendMessageOptions). */
data class OutgoingRequest(
    val message: String? = null,
    val files: List<com.eka.conversation.matrix.media.MatrixFile>? = null,
    val audio: com.eka.conversation.matrix.media.AudioPayload? = null,
    val toolDeclined: Boolean = false,
    val toolId: String? = null,
    val toolResult: JsonObject? = null,
    val initialPrompts: List<Pair<String, String>>? = null,
    val urls: List<String>? = null,
)
