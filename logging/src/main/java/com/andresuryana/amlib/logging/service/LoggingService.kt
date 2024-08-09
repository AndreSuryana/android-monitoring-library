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
import com.andresuryana.amlib.core.di.CoreDependencies
import com.andresuryana.amlib.logging.LogLevel.Companion.isLogLevelSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.GZIPOutputStream

/**
 * A service that manages log operations, including handling log messages,
 * connecting to necessary services, and ensuring messages are processed and sent
 * according to network availability and other conditions.
 */
class LoggingService : Service() {

    // Lazy initialization of the IMessagingClient instance
    private val messageClient: IMessagingClient by lazy {
        CoreDependencies.getInstance(context = this.applicationContext)
            .provideIMessagingClient()
    }

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var deviceId: String? = null

    private val logQueue = ConcurrentLinkedQueue<String>()
    private val retryQueue = ConcurrentLinkedQueue<List<String>>()
    private var isConnected = false

    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Called when the service is first created.
     * Registers network callbacks and initiates batch processing of log messages.
     */
    override fun onCreate() {
        super.onCreate()

        // Register network callback to monitor connectivity changes
        registerNetworkCallback()

        // Launch coroutine to connect to the server when the service starts
        coroutineScope.launch { connectToServer() }

        // Start batching logs
        coroutineScope.launch { processLogQueue() }
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
            Log.e(TAG, "Invalid log level: $level")
            return START_STICKY
        }

        // Validate that a device ID is provided
        if (deviceId.isNullOrEmpty()) {
            Log.e(TAG, "Device ID cannot be empty")
            return START_STICKY
        }

        // Format and add the log message to the queue
        val logMessage = formatLogMessage(level, tag, message)
        logQueue.add(logMessage)

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
            val timeZoneDisplayName =
                timeZone.getDisplayName(timeZone.inDaylightTime(date), TimeZone.SHORT, Locale.US)
            timestamp = "$timestamp $timeZoneDisplayName"
        }
        return "$timestamp [$level] $tag: $message"
    }

    /**
     * Connects to the messaging server and starts processing the log queue.
     */
    private suspend fun connectToServer() {
        try {
            if (!messageClient.isConnected()) {
                messageClient.connect() // Attempt to connect to the server
            }
            isConnected = true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to server: ${e.message}\n${Log.getStackTraceString(e)}")
        }
    }

    /**
     * Disconnects from the messaging server.
     */
    private fun disconnectFromServer() {
        coroutineScope.launch {
            try {
                if (messageClient.isConnected()) {
                    messageClient.disconnect() // Attempt to disconnect from the server
                }
                isConnected = false
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting from server: ${e.message}")
            }
        }
    }

    /**
     * Processes the log queue, attempting to publish each log message to the server.
     * Retries publishing up to a maximum number of times if it fails.
     */
    private suspend fun processLogQueue() {
        while (true) {
            // Wait for a specified interval before processing the log queue
            delay(BATCH_INTERVAL)

            // Check if the connection is available and the queue is not empty
            if (isConnected && logQueue.isNotEmpty()) {
                // Create a batch of log messages
                val batch = mutableListOf<String>()
                while (batch.size < BATCH_SIZE && logQueue.isNotEmpty()) {
                    logQueue.poll()?.let { batch.add(it) }
                }

                if (batch.isNotEmpty()) {
                    if (!processBatch(batch)) {
                        retryQueue.add(batch) // Add failed batch to retry queue
                    }
                }
            }

            // Retry failed batches
            retryFailedBatches()
        }
    }

    /**
     * Processes a batch of log messages and publishes them to the server.
     */
    private suspend fun processBatch(batch: List<String>): Boolean {
        var retryCount = 0
        var success = false

        while (retryCount < MAX_RETRIES && !success) {
            try {
                if (!messageClient.isConnected()) {
                    messageClient.connect()
                }

                val routingKey = "log.$deviceId"
                messageClient.publish(routingKey, compressLogs(batch))
                success = true
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error publishing log batch: ${e.message}\n${Log.getStackTraceString(e)}"
                )
                retryCount++
                delay(RETRY_INTERVAL)
            }
        }

        return success
    }

    /**
     * Retries failed batches from the retry queue.
     */
    private suspend fun retryFailedBatches() {
        while (retryQueue.isNotEmpty()) {
            retryQueue.poll()?.let { batch ->
                if (!processBatch(batch)) {
                    retryQueue.add(batch) // Add failed batch back to retry queue
                }
            }
        }
    }

    /**
     * Compresses a list of log messages using GZIP compression.
     */
    private fun compressLogs(logs: List<String>): String {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).bufferedWriter().use { writer ->
            logs.forEach { writer.write(it); writer.write("\n") }
        }
        return output.toString("ISO-8859-1")
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
        connectivityManager.registerNetworkCallback(
            networkRequest,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    isConnected = true
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

        private const val BATCH_INTERVAL = 10000L // Time interval to send the batch
        private const val BATCH_SIZE = 20 // Maximum number of messages in the batch

        const val EXTRA_TAG = "tag"
        const val EXTRA_LEVEL = "level"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_DEVICE_ID = "device_id"
    }
}