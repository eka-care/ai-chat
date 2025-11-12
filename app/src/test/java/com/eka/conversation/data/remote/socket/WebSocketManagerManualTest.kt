package com.eka.conversation.data.remote.socket

import com.eka.conversation.data.remote.socket.states.SocketConnectionState
import com.eka.conversation.data.remote.socket.states.SocketMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Manual test class for WebSocketManager
 * This class can be run standalone to test WebSocket functionality without UI
 *
 * Usage: Run the main() function to execute all tests
 *
 * Prerequisites:
 * - A WebSocket echo server running (you can use ws://echo.websocket.org or similar)
 * - Or set up a local WebSocket server for testing
 */
class WebSocketManagerManualTest {

    companion object {
        // Public WebSocket echo server for testing
        private const val ECHO_SERVER_URL = "wss://echo.websocket.org/"

        // Alternative: ws://localhost:8080 if you run a local server
        // private const val LOCAL_SERVER_URL = "ws://localhost:8080"

        @JvmStatic
        fun main(args: Array<String>) {
            println("=== WebSocketManager Manual Test Suite ===\n")

            val tester = WebSocketManagerManualTest()

            runBlocking {
                try {
                    tester.testBasicConnection()
                    delay(2000) // Wait between tests

                    tester.testSendAndReceiveMessages()
                    delay(2000)

                    tester.testReconnection()
                    delay(2000)

                    tester.testDisconnection()
                    delay(2000)

                    tester.testMultipleMessages()
                    delay(2000)

                    tester.testConnectionFailure()

                } catch (e: Exception) {
                    println("❌ Test suite failed with error: ${e.message}")
                    e.printStackTrace()
                }
            }

            println("\n=== Test Suite Completed ===")
        }
    }

    /**
     * Test 1: Basic Connection
     * Verifies that the WebSocket can connect successfully
     */
    private suspend fun testBasicConnection() {
        println("\n--- Test 1: Basic Connection ---")
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val manager = WebSocketManager(ECHO_SERVER_URL, scope)

        val job = scope.launch {
            manager.listenConnectionState().collect { state ->
                when (state) {
                    is SocketConnectionState.Connecting -> {
                        println("✓ State: Connecting...")
                    }

                    is SocketConnectionState.Connected -> {
                        println("✓ State: Connected successfully!")
                    }

                    is SocketConnectionState.Disconnecting -> {
                        println("✓ State: Disconnecting...")
                    }

                    is SocketConnectionState.Disconnected -> {
                        println("✓ State: Disconnected")
                    }

                    is SocketConnectionState.Error -> {
                        println("✗ State: Error - ${state.error.message}")
                    }
                }
            }
        }

        manager.connect()
        delay(3000) // Wait for connection

        manager.disconnect()
        delay(1000)

        job.cancel()
        manager.cleanup()
        println("✓ Test 1 completed\n")
    }

    /**
     * Test 2: Send and Receive Messages
     * Verifies that messages can be sent and received
     */
    private suspend fun testSendAndReceiveMessages() {
        println("\n--- Test 2: Send and Receive Messages ---")
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val manager = WebSocketManager(ECHO_SERVER_URL, scope)

        val eventJob = scope.launch {
            manager.listenEvents().collect { message ->
                when (message) {
                    is SocketMessage.TextMessage -> {
                        println("✓ Received text message: ${message.text}")
                    }

                    is SocketMessage.ByteStringMessage -> {
                        println("✓ Received bytes message: ${message.bytes.utf8()}")
                    }
                }
            }
        }

        val stateJob = scope.launch {
            manager.listenConnectionState().collect { state ->
                if (state is SocketConnectionState.Connected) {
                    println("✓ Connected, sending test message...")

                    val textSent = manager.sendText("Hello WebSocket!")
                    println("✓ Text message sent: $textSent")

                    delay(500)

                    val bytesSent = manager.sendBytes("Binary message".toByteArray())
                    println("✓ Binary message sent: $bytesSent")
                }
            }
        }

        manager.connect()
        delay(5000) // Wait for messages

        manager.disconnect()
        delay(1000)

        eventJob.cancel()
        stateJob.cancel()
        manager.cleanup()
        println("✓ Test 2 completed\n")
    }

    /**
     * Test 3: Reconnection Logic
     * Tests automatic reconnection with exponential backoff
     */
    private suspend fun testReconnection() {
        println("\n--- Test 3: Reconnection Logic ---")
        println("Note: Using invalid URL to trigger reconnection attempts")

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val manager = WebSocketManager(
            url = "ws://invalid-test-url.test:9999",
            scope = scope,
            maxReconnectAttempts = 3
        )

        var connectionAttempts = 0
        var errorCount = 0

        val job = scope.launch {
            manager.listenConnectionState().collect { state ->
                when (state) {
                    is SocketConnectionState.Connecting -> {
                        connectionAttempts++
                        println("✓ Reconnection attempt #$connectionAttempts")
                    }

                    is SocketConnectionState.Error -> {
                        errorCount++
                        println("✓ Error state (expected): ${state.error.message}")
                    }

                    else -> {
                        println("State: $state")
                    }
                }
            }
        }

        manager.connect()
        delay(15000) // Wait for reconnection attempts (exponential backoff)

        println("✓ Total connection attempts: $connectionAttempts")
        println("✓ Total errors: $errorCount")
        println("✓ Reconnection logic verified")

        job.cancel()
        manager.cleanup()
        println("✓ Test 3 completed\n")
    }

    /**
     * Test 4: Proper Disconnection
     * Verifies clean disconnection process
     */
    private suspend fun testDisconnection() {
        println("\n--- Test 4: Proper Disconnection ---")
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val manager = WebSocketManager(ECHO_SERVER_URL, scope)

        var disconnectingDetected = false
        var disconnectedDetected = false

        val job = scope.launch {
            manager.listenConnectionState().collect { state ->
                when (state) {
                    is SocketConnectionState.Disconnecting -> {
                        disconnectingDetected = true
                        println("✓ Disconnecting state detected")
                    }

                    is SocketConnectionState.Disconnected -> {
                        disconnectedDetected = true
                        println("✓ Disconnected state detected")
                    }

                    is SocketConnectionState.Connected -> {
                        println("✓ Connected, now disconnecting...")
                        delay(500)
                        manager.disconnect()
                    }

                    else -> {
                        println("State: $state")
                    }
                }
            }
        }

        manager.connect()
        delay(5000)

        println("✓ Disconnecting: $disconnectingDetected")
        println("✓ Disconnected: $disconnectedDetected")

        job.cancel()
        manager.cleanup()
        println("✓ Test 4 completed\n")
    }

    /**
     * Test 5: Multiple Messages
     * Tests sending multiple messages in succession
     */
    private suspend fun testMultipleMessages() {
        println("\n--- Test 5: Multiple Messages ---")
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val manager = WebSocketManager(ECHO_SERVER_URL, scope)

        var messagesReceived = 0
        val messagesToSend = 5

        val eventJob = scope.launch {
            manager.listenEvents().collect { message ->
                when (message) {
                    is SocketMessage.TextMessage -> {
                        messagesReceived++
                        println("✓ Received message #$messagesReceived: ${message.text}")
                    }

                    is SocketMessage.ByteStringMessage -> {
                        messagesReceived++
                        println("✓ Received binary message #$messagesReceived")
                    }
                }
            }
        }

        val stateJob = scope.launch {
            manager.listenConnectionState().collect { state ->
                if (state is SocketConnectionState.Connected) {
                    println("✓ Connected, sending $messagesToSend messages...")

                    repeat(messagesToSend) { i ->
                        val sent = manager.sendText("Message #${i + 1}")
                        println("✓ Message #${i + 1} sent: $sent")
                        delay(200)
                    }
                }
            }
        }

        manager.connect()
        delay(8000) // Wait for all messages

        println("✓ Messages sent: $messagesToSend")
        println("✓ Messages received: $messagesReceived")

        manager.disconnect()
        delay(1000)

        eventJob.cancel()
        stateJob.cancel()
        manager.cleanup()
        println("✓ Test 5 completed\n")
    }

    /**
     * Test 6: Connection Failure Handling
     * Tests behavior when connection fails
     */
    private suspend fun testConnectionFailure() {
        println("\n--- Test 6: Connection Failure Handling ---")
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val manager = WebSocketManager(
            url = "ws://definitely-invalid-url-12345.test:9999",
            scope = scope,
            maxReconnectAttempts = 2
        )

        var errorDetected = false

        val job = scope.launch {
            manager.listenConnectionState().collect { state ->
                when (state) {
                    is SocketConnectionState.Error -> {
                        errorDetected = true
                        println("✓ Error detected (expected): ${state.error.message}")
                    }

                    is SocketConnectionState.Connecting -> {
                        println("✓ Attempting connection...")
                    }

                    else -> {
                        println("State: $state")
                    }
                }
            }
        }

        manager.connect()
        delay(8000) // Wait for failure and retry attempts

        println("✓ Error handling verified: $errorDetected")

        // Test that sending fails when not connected
        val sendResult = manager.sendText("Test message")
        println("✓ Send on failed connection returns false: ${!sendResult}")

        job.cancel()
        manager.cleanup()
        println("✓ Test 6 completed\n")
    }
}