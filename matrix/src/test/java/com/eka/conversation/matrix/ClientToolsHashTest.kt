package com.eka.conversation.matrix

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tool-list fingerprint must match the web SDK byte for byte (value computed with its hash.ts). */
class ClientToolsHashTest {
    @Test fun sameHashAsWebSdk() {
        val decl = ClientToolDeclaration(
            name = "get_patient_context",
            description = "Patient profile and recent records — für Ärzte",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject { put("patient_oid", buildJsonObject { put("type", "string") }) })
            },
            title = "Reading patient",
        )
        val json = kotlinx.serialization.json.JsonArray(listOf(decl.toJson()))
        assertEquals(
            """[{"annotations":{"readOnlyHint":true,"title":"Reading patient"},"description":"Patient profile and recent records — für Ärzte","inputSchema":{"properties":{"patient_oid":{"type":"string"}},"type":"object"},"name":"get_patient_context"}]""",
            ClientToolRuntime.canonicalJson(json),
        )
        assertEquals("5ba0c8eda5d965f5", ClientToolRuntime.fnv1a64Hex(ClientToolRuntime.canonicalJson(json)))
    }

    @Test fun keysSortedAtEveryLevel() {
        val o = buildJsonObject { put("b", 1); put("a", buildJsonObject { put("d", JsonPrimitive("x")); put("c", true) }) }
        assertEquals("""{"a":{"c":true,"d":"x"},"b":1}""", ClientToolRuntime.canonicalJson(o))
    }
}
