package com.andresuryana.amlib.core.client.mqtt

import android.util.Log
import com.andresuryana.amlib.core.IMessagingClient
import com.andresuryana.amlib.core.listener.OnSubscribeTopicListener
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val DEFAULT_EXCHANGE_NAME = "libs.monitoring"

class RabbitMQClient(
    private val host: String? = ConnectionFactory.DEFAULT_HOST,
    private val port: Int = ConnectionFactory.DEFAULT_AMQP_PORT,
    private val username: String? = ConnectionFactory.DEFAULT_USER,
    private val password: String? = ConnectionFactory.DEFAULT_PASS,
    private val exchange: String? = DEFAULT_EXCHANGE_NAME,
) : IMessagingClient {

    private var connection: Connection? = null
    private var channel: Channel? = null

    private val lock = ReentrantLock()

    @Throws(Exception::class)
    override suspend fun connect() = withContext(Dispatchers.IO) {
        // Initialize the connection and channel
        lock.withLock {
            if (connection == null || connection?.isOpen == false) {
                Log.d(
                    TAG,
                    "Connecting to RabbitMQ server...\nHost: $host\nPort: $port\nUsername: $username\nPassword: $password\nExchange: $exchange"
                )
                connection = ConnectionFactory().apply {
                    host = this@RabbitMQClient.host
                    port = this@RabbitMQClient.port
                    username = this@RabbitMQClient.username
                    password = this@RabbitMQClient.password
                }.newConnection()

                channel = connection?.createChannel()?.apply {
                    exchangeDeclare(exchange, "topic", true)
                }

                Log.i(TAG, "Successfully connected to RabbitMQ server.")
            }
        }
    }

    override suspend fun publish(topic: String, payload: String): Boolean =
        withContext(Dispatchers.IO) {
            if (channel == null) {
                Log.e(
                    TAG,
                    "Failed to publish topic '${topic}' with payload '${payload}'. RabbitMQ channel is not initialized."
                )
                return@withContext false
            }

            return@withContext try {
                val props = BasicProperties.Builder().apply {
                    deliveryMode(2) // Persistent message
                }.build()
                channel?.basicPublish(exchange, topic, props, payload.toByteArray())
                Log.d(TAG, "Successfully published topic '${topic}' with payload '${payload}'.")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to publish topic '${topic}' with payload '${payload}'.", e)
                false
            }
        }

    override suspend fun subscribe(topic: String, listener: OnSubscribeTopicListener) =
        withContext(Dispatchers.IO) {
            if (channel == null) {
                Log.e(
                    TAG,
                    "Failed to subscribe to topic '${topic}'. RabbitMQ channel is not initialized."
                )
                return@withContext
            }

            try {
                channel?.basicConsume(topic, true,
                    { _, delivery ->
                        listener.onReceive(topic, String(delivery.body))
                        Log.d(TAG, "Received message on topic '${topic}': ${String(delivery.body)}")
                    }, { _ ->
                        listener.onCancel()
                        Log.w(TAG, "Consumer cancelled for topic '${topic}'.")
                    })
            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to topic '${topic}'.", e)
            }
        }

    override suspend fun unsubscribe(topic: String): Boolean = withContext(Dispatchers.IO) {
        if (channel == null) {
            Log.e(
                TAG,
                "Failed to subscribe to topic '${topic}'. RabbitMQ channel is not initialized."
            )
            return@withContext false
        }

        return@withContext try {
            channel?.basicCancel(topic)
            Log.d(TAG, "Successfully unsubscribed from topic '${topic}'.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unsubscribe from topic '${topic}'.", e)
            false
        }
    }

    @Throws(Exception::class)
    override fun disconnect() {
        lock.withLock {
            channel?.close()
            connection?.close()
            channel = null
            connection = null
        }
        Log.i(TAG, "Successfully disconnected from RabbitMQ server.")
    }

    override fun isConnected(): Boolean = connection?.isOpen == true

    override fun bindQueue(queue: String, routingKey: String) {
        // Declare and bind queue
        channel?.queueDeclare(queue, true, false, false, null)
        channel?.queueBind(queue, exchange, routingKey)
    }

    companion object {
        private val TAG: String = RabbitMQClient::class.java.simpleName
    }
}