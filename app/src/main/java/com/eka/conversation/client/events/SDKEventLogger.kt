package com.eka.conversation.client.events

import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Central event logging manager for the SDK
 * Thread-safe singleton that manages event listeners and broadcasts events
 */
object SDKEventLogger {
    private val listeners = CopyOnWriteArrayList<SDKEventListener>()
    private var isEnabled = true

    /**
     * Register an event listener to receive SDK events
     * @param listener The listener to register
     */
    fun addListener(listener: SDKEventListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    /**
     * Unregister an event listener
     * @param listener The listener to remove
     */
    fun removeListener(listener: SDKEventListener) {
        listeners.remove(listener)
    }

    /**
     * Remove all registered listeners
     */
    fun removeAllListeners() {
        listeners.clear()
    }

    /**
     * Enable or disable event logging
     * @param enabled true to enable, false to disable
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * Check if event logging is enabled
     * @return true if enabled, false otherwise
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * Log an event with the specified parameters
     * @param eventType The category/module of the event
     * @param logLevel The severity level
     * @param params JSONObject with event-specific data
     */
    fun logEvent(
        eventType: SDKEventType,
        logLevel: SDKLogLevel,
        params: JSONObject = JSONObject()
    ) {
        if (!isEnabled) return

        val event = SDKEvent(
            eventType = eventType,
            logLevel = logLevel,
            timestamp = System.currentTimeMillis(),
            params = params
        )

        notifyListeners(event)
    }

    /**
     * Log an event with builder pattern
     * @param eventType The category/module of the event
     * @param logLevel The severity level
     * @param builder Lambda to build the params JSONObject
     */
    fun logEvent(
        eventType: SDKEventType,
        logLevel: SDKLogLevel,
        builder: JSONObject.() -> Unit
    ) {
        if (!isEnabled) return

        val params = JSONObject().apply(builder)
        logEvent(eventType, logLevel, params)
    }

    /**
     * Convenience method to log DEBUG level events
     */
    fun debug(eventType: SDKEventType, params: JSONObject = JSONObject()) {
        logEvent(eventType, SDKLogLevel.DEBUG, params)
    }

    /**
     * Convenience method to log DEBUG level events with builder
     */
    fun debug(eventType: SDKEventType, builder: JSONObject.() -> Unit) {
        logEvent(eventType, SDKLogLevel.DEBUG, builder)
    }

    /**
     * Convenience method to log INFO level events
     */
    fun info(eventType: SDKEventType, params: JSONObject = JSONObject()) {
        logEvent(eventType, SDKLogLevel.INFO, params)
    }

    /**
     * Convenience method to log INFO level events with builder
     */
    fun info(eventType: SDKEventType, builder: JSONObject.() -> Unit) {
        logEvent(eventType, SDKLogLevel.INFO, builder)
    }

    /**
     * Convenience method to log WARNING level events
     */
    fun warning(eventType: SDKEventType, params: JSONObject = JSONObject()) {
        logEvent(eventType, SDKLogLevel.WARNING, params)
    }

    /**
     * Convenience method to log WARNING level events with builder
     */
    fun warning(eventType: SDKEventType, builder: JSONObject.() -> Unit) {
        logEvent(eventType, SDKLogLevel.WARNING, builder)
    }

    /**
     * Convenience method to log ERROR level events
     */
    fun error(eventType: SDKEventType, params: JSONObject = JSONObject()) {
        logEvent(eventType, SDKLogLevel.ERROR, params)
    }

    /**
     * Convenience method to log ERROR level events with builder
     */
    fun error(eventType: SDKEventType, builder: JSONObject.() -> Unit) {
        logEvent(eventType, SDKLogLevel.ERROR, builder)
    }

    private fun notifyListeners(event: SDKEvent) {
        listeners.forEach { listener ->
            try {
                listener.onEvent(event)
            } catch (e: Exception) {
                // Silently catch exceptions from listeners to prevent SDK crashes
                e.printStackTrace()
            }
        }
    }
}