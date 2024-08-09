package com.andresuryana.amlib.core.di.factory

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RestrictTo
import com.andresuryana.amlib.core.IMessagingClient
import com.andresuryana.amlib.core.client.mqtt.RabbitMQClient

@RestrictTo(RestrictTo.Scope.LIBRARY)
object IMessagingClientFactory {

    /**
     * Creates and provides an instance of [IMessagingClient].
     *
     * This method retrieves necessary configuration details from the application metadata (e.g., MQTT host,
     * port, username, password, and exchange) and uses them to initialize a [RabbitMQClient] instance.
     *
     * @param context The [Context] used to access application metadata for configuring the [IMessagingClient].
     * @return An instance of [IMessagingClient] configured with the retrieved metadata.
     * @throws PackageManager.NameNotFoundException if the application package name cannot be found.
     */
    fun create(context: Context): IMessagingClient {
        try {
            val packageManager = context.packageManager
            val appInfo =
                packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val metaData = appInfo.metaData

            val host = metaData.getString("com.andresuryana.amlib.MQTT_HOST")
            val port = metaData.getInt("com.andresuryana.amlib.MQTT_PORT")
            val username = metaData.getString("com.andresuryana.amlib.MQTT_USER")
            val password = metaData.getString("com.andresuryana.amlib.MQTT_PASS")
            val exchange = metaData.getString("com.andresuryana.amlib.MQTT_EXCHANGE")

            return RabbitMQClient(host, port, username, password, exchange)
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException("Failed to retrieve application metadata", e)
        }
    }
}