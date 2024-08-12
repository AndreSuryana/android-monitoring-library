package com.andresuryana.amlib.core.client.mqtt

import android.util.Log
import com.andresuryana.amlib.core.IMessagingClient
import com.andresuryana.amlib.core.client.mqtt.exception.MaxConnectionAttemptsException
import com.andresuryana.amlib.core.listener.OnSubscribeTopicListener
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
        Log.d(TAG, "Connecting to RabbitMQ server...\nHost: $host\nPort: $port\nUsername: $username\nPassword: $password\nExchange: $exchange")
        if (connection == null || connection?.isOpen == false) {
            val factory = ConnectionFactory().apply {
                host = this@RabbitMQClient.host
                port = this@RabbitMQClient.port
                username = this@RabbitMQClient.username
                password = this@RabbitMQClient.password
                connectionTimeout = CONNECTION_TIMEOUT
            }

            repeat(MAX_CONNECT_ATTEMPT) { attempt ->
                try {
                    lock.withLock {
                        connection = factory.newConnection()
                        channel = connection?.createChannel()?.apply {
                            exchangeDeclare(exchange, "topic", true)
                        }
                        Log.i(TAG, "Successfully connected to RabbitMQ server.")
                    }
                    return@withContext
                } catch (e: SocketTimeoutException) {
                    Log.e(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
                    if (attempt == MAX_CONNECT_ATTEMPT - 1) {
                        throw MaxConnectionAttemptsException("Max connection attempts reached.", e)
                    }
                    delay(RETRY_DELAY)
                } catch (e: Exception) {
                    Log.e(TAG, "General error: ${e.message}\n${Log.getStackTraceString(e)}")
                }
            }
        }
    }

    override suspend fun publish(topic: String, payload: String): Boolean =
        withContext(Dispatchers.IO) {
            if (channel == null) {
                Log.e(TAG, "Failed to publish topic '${topic}' with payload '${payload}'. RabbitMQ channel is not initialized.")
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

        private const val DEFAULT_EXCHANGE_NAME = "libs.monitoring"

        private const val CONNECTION_TIMEOUT = 30_000 // 30 seconds
        private const val RETRY_DELAY = 5_000L // 5 seconds
        private const val MAX_CONNECT_ATTEMPT = 3
    }
}