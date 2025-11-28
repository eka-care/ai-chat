package com.eka.conversation.client.events

enum class SDKEventType {
    // WebSocket/Network related events
    SOCKET,

    // Session management events
    SESSION_MANAGEMENT,

    // Message/Chat events
    MESSAGE,

    // Audio related events
    AUDIO,

    // Database operations
    DATABASE,

    // Network API calls
    NETWORK,

    // SDK lifecycle events
    SDK
}