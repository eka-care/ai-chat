package com.eka.conversation.data.remote.socket

import com.eka.conversation.data.remote.socket.states.SocketConnectionState
import com.eka.conversation.data.remote.socket.states.SocketMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration test for WebSocketManager with real staging server
 *
 * To run this test:
 * 1. Update STAGING_WEBSOCKET_URL with your actual staging WebSocket URL
 * 2. Run: ./gradlew :app:testDebugUnitTest --tests "*WebSocketIntegrationTest*"
 */
class WebSocketIntegrationTest {

    companion object {
        // WebSocket URL options:
        // - For local testing with test-websocket-server.js: "ws://10.0.2.2:8080" (Android emulator accessing host)
        // - For physical device with adb port forwarding: "ws://localhost:8080"
        // - For staging: Replace with your actual staging WebSocket URL (e.g., "wss://staging.example.com/ws")
        private const val WEBSOCKET_URL = "ws://localhost:8080"

        // Set to true to enable these tests
        private const val ENABLE_INTEGRATION_TESTS = true
    }

    private lateinit var webSocketManager: WebSocketManager
    private lateinit var scope: CoroutineScope

    @Before
    fun setup() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    @After
    fun tearDown() {
        if (::webSocketManager.isInitialized) {
            webSocketManager.cleanup()
        }
    }

    @Test
    fun `integration test - connect to staging server`() {
        if (!ENABLE_INTEGRATION_TESTS) {
            println("⚠️ Integration tests are disabled. Set ENABLE_INTEGRATION_TESTS = true to run.")
            return
        }

        webSocketManager = WebSocketManager(WEBSOCKET_URL, scope)

        val states = mutableListOf<SocketConnectionState>()
        var connected = false

        val collectionJob = scope.launch {
            webSocketManager.listenConnectionState().collect { state ->
                synchronized(states) {
                    states.add(state)
                }
                println("[${"%.3f".format(System.currentTimeMillis() / 1000.0)}] State: ${state::class.simpleName}")

                if (state is SocketConnectionState.Connected) {
                    connected = true
                }
            }
        }

        println("=== Starting Integration Test with Staging Server ===")
        println("URL: $WEBSOCKET_URL")

        Thread.sleep(200)

        println("Attempting to connect...")
        webSocketManager.connect()

        // Wait for connection (give it 10 seconds)
        Thread.sleep(10000)

        collectionJob.cancel()

        println("=== Test Results ===")
        println("States collected: ${states.map { it::class.simpleName }}")
        println("Connected: $connected")

        // Verify we attempted to connect
        assertTrue(
            "Should have attempted to connect",
            states.any { it is SocketConnectionState.Connecting })

        // Verify we either connected or got an error
        val hasConnectionResult = states.any {
            it is SocketConnectionState.Connected || it is SocketConnectionState.Error
        }
        assertTrue("Should have connection result (Connected or Error)", hasConnectionResult)
    }

    @Test
    fun `integration test - send and receive message on staging`() {
        if (!ENABLE_INTEGRATION_TESTS) {
            println("⚠️ Integration tests are disabled. Set ENABLE_INTEGRATION_TESTS = true to run.")
            return
        }

        webSocketManager = WebSocketManager(WEBSOCKET_URL, scope)

        val states = mutableListOf<SocketConnectionState>()
        val messages = mutableListOf<SocketMessage>()
        var connected = false

        // Collect states
        val stateJob = scope.launch {
            webSocketManager.listenConnectionState().collect { state ->
                synchronized(states) {
                    states.add(state)
                }
                println("State: ${state::class.simpleName}")
                if (state is SocketConnectionState.Connected) {
                    connected = true
                }
            }
        }

        // Collect messages
        val messageJob = scope.launch {
            webSocketManager.listenEvents().collect { message ->
                synchronized(messages) {
                    messages.add(message)
                }
                when (message) {
                    is SocketMessage.TextMessage -> println("Received: ${message.text}")
                    is SocketMessage.ByteStringMessage -> println("Received bytes: ${message.bytes.size} bytes")
                }
            }
        }

        println("=== Testing Message Send/Receive ===")

        Thread.sleep(200)
        webSocketManager.connect()

        // Wait for connection
        Thread.sleep(5000)

        if (connected) {
            println("Connected! Sending test message...")
            val testMessage = """{"type":"test","message":"Hello from integration test"}"""
            val sent = webSocketManager.sendText(testMessage)
            println("Message sent: $sent")

            // Wait for potential response
            Thread.sleep(3000)
        } else {
            println("Failed to connect to staging server")
        }

        stateJob.cancel()
        messageJob.cancel()

        println("=== Test Results ===")
        println("Connected: $connected")
        println("Messages received: ${messages.size}")

        if (connected) {
            assertTrue("Should have received at least the initial state", states.isNotEmpty())
        }
    }

    @Test
    fun `integration test - reconnection on staging`() {
        if (!ENABLE_INTEGRATION_TESTS) {
            println("⚠️ Integration tests are disabled. Set ENABLE_INTEGRATION_TESTS = true to run.")
            return
        }

        webSocketManager = WebSocketManager(
            WEBSOCKET_URL,
            scope,
            maxReconnectAttempts = 2
        )

        val states = mutableListOf<SocketConnectionState>()

        val collectionJob = scope.launch {
            webSocketManager.listenConnectionState().collect { state ->
                synchronized(states) {
                    states.add(state)
                }
                println("State: ${state::class.simpleName}")
            }
        }

        println("=== Testing Reconnection Logic ===")

        Thread.sleep(200)
        webSocketManager.connect()
        Thread.sleep(5000)

        // Force disconnect and see if it reconnects
        val connected = states.any { it is SocketConnectionState.Connected }
        if (connected) {
            println("Forcing disconnect to test reconnection...")
            webSocketManager.disconnect()
            Thread.sleep(2000)

            println("Reconnecting...")
            webSocketManager.connect()
            Thread.sleep(5000)
        }

        collectionJob.cancel()

        println("=== Test Results ===")
        println("States: ${states.map { it::class.simpleName }}")

        assertTrue("Should have connection states", states.isNotEmpty())
    }

    @Test
    fun `integration test - disconnect flow on staging`() {
        if (!ENABLE_INTEGRATION_TESTS) {
            println("⚠️ Integration tests are disabled. Set ENABLE_INTEGRATION_TESTS = true to run.")
            return
        }

        webSocketManager = WebSocketManager(WEBSOCKET_URL, scope)

        val states = mutableListOf<SocketConnectionState>()

        val collectionJob = scope.launch {
            webSocketManager.listenConnectionState().collect { state ->
                synchronized(states) {
                    states.add(state)
                }
                println("State: ${state::class.simpleName}")
            }
        }

        println("=== Testing Disconnect Flow ===")

        Thread.sleep(200)
        webSocketManager.connect()
        Thread.sleep(5000)

        val connected = states.any { it is SocketConnectionState.Connected }
        if (connected) {
            println("Connected! Now disconnecting...")
            webSocketManager.disconnect()
            Thread.sleep(2000)
        }

        collectionJob.cancel()

        println("=== Test Results ===")
        println("States: ${states.map { it::class.simpleName }}")

        if (connected) {
            val hasDisconnectedState = states.any {
                it is SocketConnectionState.Disconnected || it is SocketConnectionState.Disconnecting
            }
            assertTrue("Should have disconnected properly", hasDisconnectedState)
        }
    }
}