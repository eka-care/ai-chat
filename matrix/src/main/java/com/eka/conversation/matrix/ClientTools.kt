package com.eka.conversation.matrix

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Client tools: functions that live in the app and that the agent (via Matrix) can call, e.g.
 * get_patient_context. Same protocol as the web SDK (synapse-sdk feature/life-3048): MCP
 * `tools/call` JSON-RPC 2.0 messages inside `ct: "mcp"` chat frames. The tool list is declared in
 * the session-create context and referenced by hash on every user message.
 */
data class ClientToolDeclaration(
    /** ^[a-zA-Z0-9_-]{1,64}$ */
    val name: String,
    val description: String,
    /** JSON Schema of the arguments. */
    val inputSchema: JsonObject,
    /** Label shown while it runs, e.g. "Reading the patient's records". */
    val title: String? = null,
    val readOnly: Boolean = true,
) {
    internal fun toJson(): JsonObject = buildJsonObject {
        put("name", name)
        put("description", description)
        put("inputSchema", inputSchema)
        if (title != null || readOnly) put("annotations", buildJsonObject {
            title?.let { put("title", it) }
            put("readOnlyHint", readOnly)
        })
    }
}

/** Implemented by the host app. */
interface ClientToolHandler {
    val tools: List<ClientToolDeclaration>

    /**
     * Run [name] with [arguments] and return the result object the agent reads.
     * Throw [ClientToolException] (or anything) for a failure the agent should see.
     */
    suspend fun call(name: String, arguments: JsonObject): JsonObject
}

/** A tool failure with a code the agent can act on (e.g. "no_patient"). */
class ClientToolException(val code: String, message: String) : Exception(message)

/** Runs incoming tool calls and always answers (result, error, or nothing when the server cancelled). */
internal class ClientToolRuntime(
    private val scope: CoroutineScope,
    private val send: (JsonObject) -> Unit,
) {
    var handler: ClientToolHandler? = null
        set(value) {
            field = value
            cachedHash = null
            lastSentHash = null
        }

    private val inflight = HashMap<String, Job>()
    private val seen = LinkedHashSet<String>()
    private var cachedHash: String? = null
    private var lastSentHash: String? = null
    private var everDeclared = false

    val hasTools get() = handler?.tools?.isNotEmpty() == true

    /** Sent in session_context on every create: tells Matrix this client speaks client tools v1. */
    fun capabilities(): JsonObject = buildJsonObject {
        put("client_tools", buildJsonObject {
            put("version", PROTOCOL_VERSION)
            put("max_result_bytes", MAX_RESULT_BYTES)
            put("cancel", true)
        })
    }

    fun declarations(): JsonArray =
        JsonArray(handler?.tools.orEmpty().sortedBy { it.name }.map { it.toJson() })

    fun hash(): String = cachedHash ?: fnv1a64Hex(canonicalJson(declarations())).also { cachedHash = it }

    /** Full list for session create (null when the app has no tools). */
    fun declarationsForCreate(): JsonObject? {
        if (!hasTools) return null
        val h = hash()
        markSent(h)
        return buildJsonObject { put("hash", h); put("tools", declarations()) }
    }

    /** For each user message: the full list when it changed or after a reconnect, else the hash. */
    fun declarationsForSend(): JsonObject? {
        if (!hasTools && !everDeclared) return null
        val h = hash()
        return if (h != lastSentHash) {
            markSent(h)
            buildJsonObject { put("hash", h); put("tools", declarations()) }
        } else buildJsonObject { put("hash", h) }
    }

    private fun markSent(hash: String) {
        lastSentHash = hash
        everDeclared = true
    }

    /** Reconnect / resumed session: the next message carries the full list again. */
    fun markUnsent() {
        lastSentHash = null
    }

    /** A `ct: "mcp"` frame's data. */
    fun handle(data: JsonElement?) {
        val msg = data as? JsonObject ?: return
        when (msg.str("method")) {
            METHOD_CANCELLED -> {
                val requestId = (msg["params"] as? JsonObject)?.get("requestId")?.let(::idKey) ?: return
                MatrixLog.d("client tool cancelled by server: $requestId")
                inflight.remove(requestId)?.cancel(CancellationException("backend_cancelled"))
            }
            METHOD_TOOLS_CALL -> {
                val id = msg["id"] as? JsonPrimitive ?: return
                val key = idKey(id)
                if (!seen.add(key)) return // re-delivered after a reconnect: never run twice
                while (seen.size > SEEN_LIMIT) seen.remove(seen.first())
                val params = msg["params"] as? JsonObject ?: return
                val name = params.str("name") ?: return
                val args = params["arguments"] as? JsonObject ?: JsonObject(emptyMap())
                inflight[key] = scope.launch { execute(id, key, name, args) }
            }
        }
    }

    private suspend fun execute(id: JsonPrimitive, key: String, name: String, args: JsonObject) {
        val h = handler
        val started = System.currentTimeMillis()
        MatrixLog.d("client tool call: $name (args=${args.keys})")
        if (h == null || h.tools.none { it.name == name }) {
            reply(rpcError(id, ERR_METHOD_NOT_FOUND, "Unknown tool: $name"))
            inflight.remove(key)
            return
        }
        val reply = try {
            val value = withTimeout(TIMEOUT_MS) { h.call(name, args) }
            val result = buildJsonObject {
                put("content", JsonArray(listOf(textContent(value.toString()))))
                put("structuredContent", value)
            }
            if (result.toString().toByteArray().size > MAX_RESULT_BYTES) {
                errorResult("result_too_large", "Result of $name is larger than $MAX_RESULT_BYTES bytes")
            } else result
        } catch (e: TimeoutCancellationException) {
            errorResult("timeout", "Tool \"$name\" timed out after $TIMEOUT_MS ms")
        } catch (e: CancellationException) {
            inflight.remove(key)
            return // server cancelled (no reply, per MCP) or session ended (abortAll answered)
        } catch (e: ClientToolException) {
            errorResult(e.code, e.message ?: "Tool failed")
        } catch (e: Throwable) {
            errorResult("execution_failed", e.message ?: "Tool failed")
        }
        inflight.remove(key)
        MatrixLog.d("client tool done: $name in ${System.currentTimeMillis() - started} ms" +
            if ((reply["isError"] as? JsonPrimitive)?.content == "true") " (error)" else "")
        reply(buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", id)
            put("result", reply)
        })
    }

    /** Session ending: every pending call is answered with -32800 before the socket closes. */
    fun abortAll() {
        val pending = inflight.toMap()
        inflight.clear()
        pending.forEach { (key, job) ->
            job.cancel(CancellationException("session_ended"))
            reply(rpcError(keyToId(key), ERR_CANCELLED, "Request cancelled"))
        }
    }

    private fun reply(message: JsonObject) = try {
        send(message)
    } catch (e: Throwable) {
        MatrixLog.w("client tool reply not sent: ${e.message}")
    }

    private fun errorResult(code: String, message: String) = buildJsonObject {
        put("isError", true)
        put("content", JsonArray(listOf(textContent(message))))
        put("structuredContent", buildJsonObject { put("code", code); put("message", message) })
    }

    private fun textContent(text: String) = buildJsonObject { put("type", "text"); put("text", text) }

    private fun rpcError(id: JsonPrimitive, code: Int, message: String) = buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", id)
        put("error", buildJsonObject { put("code", code); put("message", message) })
    }

    // Ids are strings or numbers; keep the original type for the reply
    private fun idKey(id: JsonElement) = (id as? JsonPrimitive)?.let { (if (it.isString) "s:" else "n:") + it.content } ?: ""
    private fun keyToId(key: String): JsonPrimitive =
        if (key.startsWith("n:")) JsonPrimitive(key.substring(2).toLongOrNull() ?: 0L) else JsonPrimitive(key.substring(2))

    companion object {
        const val PROTOCOL_VERSION = "1"
        const val MAX_RESULT_BYTES = 262_144
        const val TIMEOUT_MS = 30_000L
        private const val SEEN_LIMIT = 200
        private const val METHOD_TOOLS_CALL = "tools/call"
        private const val METHOD_CANCELLED = "notifications/cancelled"
        private const val ERR_METHOD_NOT_FOUND = -32601
        private const val ERR_CANCELLED = -32800

        /** JSON with object keys sorted recursively (same as the web SDK's canonicalJson). */
        internal fun canonicalJson(e: JsonElement): String = when (e) {
            is JsonObject -> e.keys.sorted().joinToString(",", "{", "}") { k -> JsonPrimitive(k).toString() + ":" + canonicalJson(e.getValue(k)) }
            is JsonArray -> e.joinToString(",", "[", "]") { canonicalJson(it) }
            JsonNull -> "null"
            is JsonPrimitive -> e.toString()
        }

        /** FNV-1a 64-bit over the UTF-8 bytes, 16 hex chars (same as the web SDK). */
        internal fun fnv1a64Hex(input: String): String {
            var hash = 0xcbf29ce484222325UL
            val prime = 0x100000001b3UL
            for (b in input.toByteArray(Charsets.UTF_8)) {
                hash = hash xor (b.toUByte().toULong())
                hash *= prime
            }
            return hash.toString(16).padStart(16, '0')
        }
    }
}
