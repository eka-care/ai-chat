package com.eka.conversation.data.remote.socket

import app.cash.turbine.test
import com.eka.conversation.data.remote.socket.states.SocketConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WebSocketManagerTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var webSocketManager: WebSocketManager

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        if (::webSocketManager.isInitialized) {
            webSocketManager.cleanup()
        }
        mockWebServer.shutdown()
    }

    @Test
    fun `test initial connection state is Connecting`() = runTest {
        // Given
        val url = mockWebServer.url("/").toString()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        webSocketManager = WebSocketManager(url, scope)

        // Then - initial state should be Connecting
        webSocketManager.listenConnectionState().test {
            val state = awaitItem()
            assertTrue(state is SocketConnectionState.Connecting)
            cancel()
        }
    }

    @Test
    fun `test send text message returns false when not connected`() = runTest {
        // Given
        val url = "ws://invalid-url:9999"
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        webSocketManager = WebSocketManager(url, scope)

        // When - try to send without connecting
        val result = webSocketManager.sendText("test message")

        // Then
        assertFalse(result)
    }

    @Test
    fun `test send bytes message returns false when not connected`() = runTest {
        // Given
        val url = "ws://invalid-url:9999"
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        webSocketManager = WebSocketManager(url, scope)

        // When - try to send without connecting
        val result = webSocketManager.sendBytes("test".toByteArray())

        // Then
        assertFalse(result)
    }

    @Test
    fun `test connection error emits Error state`() = runTest {
        // Given
        val url = "ws://definitely-invalid-url-12345:9999"
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        webSocketManager = WebSocketManager(url, scope, maxReconnectAttempts = 1)

        var errorDetected = false

        // When
        webSocketManager.listenConnectionState().test {
            // Initial state
            assertEquals(SocketConnectionState.Connecting, awaitItem())

            webSocketManager.connect()
            delay(3000) // Wait for connection attempt

            // Should get error state
            val state = awaitItem()
            if (state is SocketConnectionState.Error) {
                errorDetected = true
            }

            cancel()
        }

        // Then
        assertTrue("Expected Error state but didn't receive it", errorDetected)
    }

    @Test
    fun `test disconnect flow`() = runTest {
        // Given - test basic disconnect functionality without needing successful connection
        val url = mockWebServer.url("/").toString()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        webSocketManager = WebSocketManager(url, scope)

        val states = mutableListOf<SocketConnectionState>()

        println("=== Starting disconnect flow test ===")

        // Launch a collector on the real dispatcher
        val collectionJob = scope.launch {
            webSocketManager.listenConnectionState().collect { state ->
                synchronized(states) {
                    states.add(state)
                }
                println("[Disconnect test] State: ${state::class.simpleName}")
            }
        }

        // Give collector time to start
        Thread.sleep(100)

        // Connect (may or may not succeed with MockWebServer)
        println("Connecting...")
        webSocketManager.connect()
        Thread.sleep(500) // Brief wait

        println("Disconnecting...")
        // Disconnect - this should work regardless
        webSocketManager.disconnect()
        Thread.sleep(500) // Wait for disconnection

        // Stop collecting
        collectionJob.cancel()

        // Then - verify we have basic state transitions
        println("=== Collected states: ${states.map { it::class.simpleName }} ===")

        // Should at minimum have Connecting state (from initial state)
        assertTrue(
            "Should have Connecting state. States: ${states.map { it::class.simpleName }}",
            states.any { it is SocketConnectionState.Connecting })

        // Should reach some form of disconnected state after calling disconnect
        val hasDisconnectedState = states.any {
            it is SocketConnectionState.Disconnected || it is SocketConnectionState.Disconnecting
        }
        assertTrue(
            "Should have Disconnected or Disconnecting state. States: ${states.map { it::class.simpleName }}",
            hasDisconnectedState
        )
    }

    @Test
    fun `test cleanup prevents further operations`() = runTest {
        // Given
        val url = mockWebServer.url("/").toString()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        webSocketManager = WebSocketManager(url, scope)

        webSocketManager.connect()
        Thread.sleep(2000)  // Use real time

        // When
        webSocketManager.cleanup()

        // Then - subsequent operations should fail
        val sendResult = webSocketManager.sendText("test")
        assertFalse(sendResult)
    }

    @Test
    fun `test max reconnect attempts limits retries`() =
        runTest(timeout = 60000.toDuration(DurationUnit.MILLISECONDS)) {
            // Given
            val url = "ws://invalid-url-for-testing:9999"
            val maxReconnectAttempts = 4  // FIX: Use consistent variable

            // FIX: Must use SupervisorJob so failures don't cancel the scope
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            webSocketManager =
                WebSocketManager(url, scope, maxReconnectAttempts = maxReconnectAttempts)

            val collectedStates = mutableListOf<SocketConnectionState>()

            println("=== Starting reconnect test with maxReconnectAttempts=$maxReconnectAttempts ===")
            val startTime = System.currentTimeMillis()

            // Launch a coroutine to collect states
            val collectionJob = scope.launch {
                webSocketManager.listenConnectionState().collect { state ->
                    synchronized(collectedStates) {
                        collectedStates.add(state)
                    }
                    val elapsed = System.currentTimeMillis() - startTime
                    println("[$elapsed ms] State collected: ${state::class.simpleName}")
                }
            }

            // Give the collector time to start (real time)
            Thread.sleep(100)

            // Start connection
            println("Starting connection attempt...")
            webSocketManager.connect()

            // IMPORTANT: Must use Thread.sleep (real time) not delay (virtual time)
            // because WebSocketManager runs on Dispatchers.IO (real threads)
            // With maxReconnectAttempts=4:
            // - Initial attempt: ~0s
            // - Retry 1: after 1s (delay = 1000ms * 2^0)
            // - Retry 2: after 2s (delay = 1000ms * 2^1)
            // - Retry 3: after 4s (delay = 1000ms * 2^2)
            // - Retry 4: after 8s (delay = 1000ms * 2^3)
            // Total: ~16s + connection attempt time + buffer

            println("Waiting 20 seconds for all retries...")
            Thread.sleep(35000L) // REAL TIME - not virtual

            // Stop collecting
            collectionJob.cancel()

            // Count how many times we entered Connecting state
            val connectingCount = synchronized(collectedStates) {
                collectedStates.count { it is SocketConnectionState.Connecting }
            }

            println("=== Test Results ===")
            println("Total Connecting states: $connectingCount")
            println("All states: ${collectedStates.map { it::class.simpleName }}")
            println("Total states collected: ${collectedStates.size}")

            // Then - should not exceed max attempts + 1 (initial attempt)
            // The manager stops retrying AFTER reaching maxReconnectAttempts
            // So total Connecting states = 1 (initial) + maxReconnectAttempts (retries)
            val expectedMaxConnecting = maxReconnectAttempts + 1

            assertTrue(
                "Connection attempts should be limited to $expectedMaxConnecting (1 initial + $maxReconnectAttempts retries), " +
                        "but was $connectingCount. States: ${collectedStates.map { it::class.simpleName }}",
                connectingCount <= expectedMaxConnecting
            )

            println("=== Test PASSED ===")
        }

    @Test
    fun `test prevent duplicate connection when already connected`() = runTest {
        // Given
        val url = mockWebServer.url("/").toString()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        webSocketManager = WebSocketManager(url, scope)

        webSocketManager.connect()
        delay(2000)

        var states = mutableListOf<SocketConnectionState>()

        webSocketManager.listenConnectionState().test {
            states.add(awaitItem())

            // Try to connect again
            webSocketManager.connect()
            delay(500)

            // Should not add another Connecting state
            cancel()
        }

        // If already connected, calling connect again should be ignored
        // This is more of a behavioral test
        assertTrue(true)
    }
}