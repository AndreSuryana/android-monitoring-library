package com.andresuryana.monitoringlibrary.logging.service

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.andresuryana.monitoringlibrary.common.IMessagingClient
import com.andresuryana.monitoringlibrary.logging.LogLevel.Companion.isLogLevelSupported
import com.andresuryana.monitoringlibrary.mqtt.RabbitMQClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LoggingService : Service() {

    private lateinit var messageClient: IMessagingClient
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var deviceId: String? = null

    override fun onCreate() {
        super.onCreate()

        // Retrieve RabbitMQ configuration from metadata
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val metaData = appInfo.metaData

        val host = metaData.getString("com.andresuryana.monitoringlibrary.MQTT_HOST")
        val port = metaData.getInt("com.andresuryana.monitoringlibrary.MQTT_PORT")
        val username = metaData.getString("com.andresuryana.monitoringlibrary.MQTT_USER")
        val password = metaData.getString("com.andresuryana.monitoringlibrary.MQTT_PASS")
        val exchange = metaData.getString("com.andresuryana.monitoringlibrary.MQTT_EXCHANGE")

        // Initialize RabbitMQClient with retrieved values
        messageClient = RabbitMQClient(host, port, username, password, exchange)
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
            try {
                // Check RabbitMQ server connection
                if (!messageClient.isConnected()) {
                    messageClient.connect()
                }

                // Publish the log message with the device-specific routing key
                val routingKey = "log.$deviceId"
                messageClient.publish(routingKey, logMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Error publishing log message: ${e.message}")
                e.printStackTrace()
            }
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

    companion object {

        private val TAG = LoggingService::class.java.simpleName

        const val EXTRA_TAG = "tag"
        const val EXTRA_LEVEL = "level"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_DEVICE_ID = "device_id"
    }
}