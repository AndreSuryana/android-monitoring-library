package com.andresuryana.amlib.core

import com.andresuryana.amlib.core.listener.OnSubscribeTopicListener

interface IMessagingClient {

    @Throws(Exception::class)
    suspend fun connect()

    suspend fun publish(topic: String, payload: String): Boolean

    suspend fun subscribe(topic: String, listener: OnSubscribeTopicListener)

    suspend fun unsubscribe(topic: String): Boolean

    @Throws(Exception::class)
    fun disconnect()

    fun isConnected(): Boolean

    fun bindQueue(queue: String, routingKey: String)
}