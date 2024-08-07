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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import java.util.TimeZone
import javax.inject.Inject

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

    override fun onCreate() {
        super.onCreate()
        registerNetworkCallback()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                messageClient.connect()
                isConnected = true
                processLogQueue()
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to RabbitMQ server: ${e.message}\n${Log.getStackTraceString(e)}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Retrieve the intent data
        val tag = intent?.getStringExtra(EXTRA_TAG) ?: ""
        val level = intent?.getStringExtra(EXTRA_LEVEL) ?: ""
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: ""
        deviceId = intent?.getStringExtra(EXTRA_DEVICE_ID)

        // Validate log level
        if (!isLogLevelSupported(level)) {
            throw IllegalArgumentException("Invalid log level: $level")
        }

        // Validate device id
        if (deviceId.isNullOrEmpty()) {
            throw IllegalArgumentException("Device ID cannot be empty")
        }

        // Format log message
        val date = Date()
        var timestamp = sdf.format(date)
        val timeZone = TimeZone.getDefault()
        if (timeZone != null) {
            val timeZoneDisplayName = timeZone.getDisplayName(timeZone.inDaylightTime(date), TimeZone.SHORT, Locale.US)
            timestamp = "$timestamp $timeZoneDisplayName"
        }
        val logMessage = "$timestamp [$level] $tag: $message"

        // Launch a coroutine to publish the log message
        CoroutineScope(Dispatchers.IO).launch {
            logQueue.add(logMessage)
            Log.d(TAG, "onStartCommand: Added log message to queue $logMessage")
            processLogQueue()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            messageClient.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from RabbitMQ server: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun processLogQueue() {
        if (isConnected) {
            while (logQueue.isNotEmpty()) {
                val logMessage = logQueue.poll() ?: continue
                try {
                    if (!messageClient.isConnected()) {
                        messageClient.connect()
                    }
                    val routingKey = "log.$deviceId"
                    messageClient.publish(routingKey, logMessage)
                } catch (e: Exception) {
                    Log.e(TAG, "Error publishing log message: ${e.message}\n${Log.getStackTraceString(e)}")
                    logQueue.add(logMessage) // Re-add the log message to the queue when publishing fails
                    Log.d(TAG, "processLogQueue: Re-add log message to queue $logMessage")
                    delay(RETRY_INTERVAL)
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                isConnected = true
                CoroutineScope(Dispatchers.IO).launch {
                    processLogQueue()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                isConnected = false
            }
        })
    }

    companion object {

        private val TAG = LoggingService::class.java.simpleName

        private const val RETRY_INTERVAL = 5000L // 5 seconds

        const val EXTRA_TAG = "tag"
        const val EXTRA_LEVEL = "level"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_DEVICE_ID = "device_id"
    }
}