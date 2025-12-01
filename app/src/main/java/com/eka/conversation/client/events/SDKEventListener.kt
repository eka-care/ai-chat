package com.eka.conversation.client.events

/**
 * Interface for clients to receive SDK events
 */
interface SDKEventListener {
    /**
     * Called whenever an SDK event occurs
     * @param event The SDK event containing event type, log level, timestamp and parameters
     */
    fun onEvent(event: SDKEvent)
}