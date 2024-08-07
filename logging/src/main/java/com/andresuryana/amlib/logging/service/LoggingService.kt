package com.andresuryana.amlib.logging.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import android.util.Log
import com.andresuryana.amlib.core.IMessagingClient
import com.andresuryana.amlib.logging.LogLevel.Companion.isLogLevelSupported
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import java.util.TimeZone
import javax.inject.Inject

/**
 * A service that manages log operations, including handling log messages,
 * connecting to necessary services, and ensuring messages are processed and sent
 * according to network availability and other conditions.
 */
@AndroidEntryPoint
class LoggingService : Service() {

    @Inject
    lateinit var messageClient: IMessagingClient

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var deviceId: String? = null

    private val logQueue: Queue<String> = LinkedList()
    private var isConnected = false

    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Called when the service is first created.
     * Registers network callbacks and initiates connection to the server.
     */
    override fun onCreate() {
        super.onCreate()

        // Register network callback to monitor connectivity changes
        registerNetworkCallback()

        // Launch coroutine to connect to the server when the service starts
        coroutineScope.launch { connectToServer() }
    }

    /**
     * Called when a client starts the service using an Intent.
     * Retrieves log details from the intent, validates them, formats the log message, and adds it to the queue.
     *
     * @param intent The intent containing the log data.
     * @param flags Additional data about the start request.
     * @param startId A unique integer representing the start request.
     * @return The start mode of the service (START_STICKY).
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Retrieve log details from the intent
        val tag = intent?.getStringExtra(EXTRA_TAG) ?: ""
        val level = intent?.getStringExtra(EXTRA_LEVEL) ?: ""
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: ""
        deviceId = intent?.getStringExtra(EXTRA_DEVICE_ID)

        // Validate the log level to ensure it is supported
        if (!isLogLevelSupported(level)) {
            throw IllegalArgumentException("Invalid log level: $level")
        }

        // Validate that a device ID is provided
        if (deviceId.isNullOrEmpty()) {
            throw IllegalArgumentException("Device ID cannot be empty")
        }

        // Format the log message with timestamp and timezone
        val logMessage = formatLogMessage(level, tag, message)

        // Add log message to queue and process it
        logQueue.add(logMessage)
        coroutineScope.launch { processLogQueue() }

        return START_STICKY
    }

    /**
     * Called when a client binds to the service.
     *
     * @param intent The intent used to bind to the service.
     * @return An IBinder for the client to interact with the service.
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Called when the service is destroyed.
     * Cancels all coroutines and disconnects from the server.
     */
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Cancel all coroutines when the service is destroyed
        disconnectFromServer() // Disconnect from the server
    }

    /**
     * Formats a log message with a timestamp and timezone.
     *
     * @param level The log level (e.g., DEBUG, INFO, ERROR).
     * @param tag The tag or category of the log message.
     * @param message The actual log message.
     * @return The formatted log message with timestamp and timezone.
     */
    private fun formatLogMessage(level: String, tag: String, message: String): String {
        val date = Date()
        var timestamp = sdf.format(date)
        val timeZone = TimeZone.getDefault()
        if (timeZone != null) {
            val timeZoneDisplayName = timeZone.getDisplayName(timeZone.inDaylightTime(date), TimeZone.SHORT, Locale.US)
            timestamp = "$timestamp $timeZoneDisplayName"
        }
        return "$timestamp [$level] $tag: $message"
    }

    /**
     * Connects to the messaging server and starts processing the log queue.
     */
    private suspend fun connectToServer() {
        try {
            messageClient.connect() // Attempt to connect to the server
            isConnected = true
            processLogQueue() // Start processing the log queue
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to server: ${e.message}\n${Log.getStackTraceString(e)}")
        }
    }

    /**
     * Disconnects from the messaging server.
     */
    private fun disconnectFromServer() {
        try {
            messageClient.disconnect() // Attempt to disconnect from the server
            isConnected = false
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from server: ${e.message}")
        }
    }

    /**
     * Processes the log queue, attempting to publish each log message to the server.
     * Retries publishing up to a maximum number of times if it fails.
     */
    private suspend fun processLogQueue() {
        // If not connected, there's no point in processing the queue
        if (!isConnected) return

        // Continue processing while there are log messages in the queue
        while (logQueue.isNotEmpty()) {
            val logMessage = logQueue.poll() ?: continue // Retrieve and remove the next log message from the queue
            var retryCount = 0 // Initialize retry count for this message
            var success = false // Flag to track if the message was successfully published

            // Retry publishing the log message up to a maximum number of times for each log message
            while (retryCount < MAX_RETRIES && !success) {
                try {
                    // Ensure the message client is connected before attempting to publish
                    if (!messageClient.isConnected()) {
                        messageClient.connect()
                    }
                    // Define the routing key for the message
                    val routingKey = "log.$deviceId"

                    // Attempt to publish the log message
                    messageClient.publish(routingKey, logMessage)
                    success = true // Mark the message as successfully published
                } catch (e: Exception) {
                    Log.e(TAG, "Error publishing log message: ${e.message}\n${Log.getStackTraceString(e)}")

                    // If publishing fails, re-add the log message to the queue for retry
                    logQueue.add(logMessage)
                    Log.d(TAG, "Retrying log message. Attempt ${retryCount + 1} of $MAX_RETRIES")

                    // Increment the retry count for this message
                    retryCount++

                    // Wait for a specified interval before retrying to avoid overwhelming the server or network
                    delay(RETRY_INTERVAL)
                }
            }
        }
    }

    /**
     * Registers a network callback to monitor network connectivity changes.
     * Updates the connection status and triggers log processing when network becomes available.
     */
    private fun registerNetworkCallback() {
        // Create a network request to monitor changes in network connectivity
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        // Register a network callback to handle connectivity changes
        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                isConnected = true

                // Process the log queue when network becomes available
                coroutineScope.launch { processLogQueue() }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                isConnected = false
            }
        })
    }

    companion object {

        private val TAG = LoggingService::class.java.simpleName

        private const val RETRY_INTERVAL = 5000L // 5 seconds between retries
        private const val MAX_RETRIES = 3 // Maximum number of retries for each log message

        const val EXTRA_TAG = "tag"
        const val EXTRA_LEVEL = "level"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_DEVICE_ID = "device_id"
    }
}