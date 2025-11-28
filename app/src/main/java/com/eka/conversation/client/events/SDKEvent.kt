package com.eka.conversation.client.events

import org.json.JSONObject

/**
 * Generic SDK Event structure
 * @param eventType The category/module of the event (e.g., SOCKET, SESSION_MANAGEMENT, MESSAGE)
 * @param logLevel The severity level of the event (DEBUG, INFO, WARNING, ERROR)
 * @param timestamp The time when the event occurred in milliseconds
 * @param params JSONObject containing all event-specific parameters
 */
data class SDKEvent(
    val eventType: SDKEventType,
    val logLevel: SDKLogLevel,
    val timestamp: Long,
    val params: JSONObject
)