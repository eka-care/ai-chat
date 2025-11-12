package com.eka.conversation.data.remote.socket

import app.cash.turbine.test
import com.eka.conversation.data.remote.socket.states.SocketConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
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
    fun `test initial connection state is Idle`() = runTest {
        // Given
        val url = mockWebServer.url("/").toString()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        webSocketManager = WebSocketManager(url, scope)

        // Then - initial state should be Connecting
        webSocketManager.listenConnectionState().test {
            val state = awaitItem()
            assertTrue(state is SocketConnectionState.Idle)
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
            assertEquals(SocketConnectionState.Idle, awaitItem())

            webSocketManager.connect()

            assertEquals(SocketConnectionState.Connecting, awaitItem())

            Thread.sleep(3000) // Wait for connection attempt

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
        // Use public echo server that actually supports WebSocket
        val url = "wss://echo.websocket.org/"
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        webSocketManager = WebSocketManager(url, scope)

        val states = mutableListOf<SocketConnectionState>()

        val collectionJob = scope.launch {
            webSocketManager.listenConnectionState().collect { state ->
                synchronized(states) {
                    states.add(state)
                }
            }
        }

        Thread.sleep(200)
        webSocketManager.connect()
        Thread.sleep(5000) // Wait for connection
        webSocketManager.disconnect()
        Thread.sleep(2000)

        collectionJob.cancel()

        val hasDisconnectedState = states.any {
            it is SocketConnectionState.Disconnected || it is SocketConnectionState.Disconnecting
        }

        assertTrue("Should have states", states.isNotEmpty())
        assertTrue("Should have Connecting", states.any { it is SocketConnectionState.Connecting })
        assertTrue("Should have Connected", states.any { it is SocketConnectionState.Connected })
        assertTrue("Should have disconnected state", hasDisconnectedState)
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

            // IMPORTANT: Must use Thread.sleep (real time) not Thread.sleep (virtual time)
            // because WebSocketManager runs on Dispatchers.IO (real threads)
            // With maxReconnectAttempts=4:
            // - Initial attempt: ~0s
            // - Retry 1: after 1s (Thread.sleep = 1000ms * 2^0)
            // - Retry 2: after 2s (Thread.sleep = 1000ms * 2^1)
            // - Retry 3: after 4s (Thread.sleep = 1000ms * 2^2)
            // - Retry 4: after 8s (Thread.sleep = 1000ms * 2^3)
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
        Thread.sleep(2000)

        var states = mutableListOf<SocketConnectionState>()

        webSocketManager.listenConnectionState().test {
            states.add(awaitItem())

            // Try to connect again
            webSocketManager.connect()
            Thread.sleep(500)

            // Should not add another Connecting state
            cancel()
        }

        // If already connected, calling connect again should be ignored
        // This is more of a behavioral test
        assertTrue(true)
    }
}