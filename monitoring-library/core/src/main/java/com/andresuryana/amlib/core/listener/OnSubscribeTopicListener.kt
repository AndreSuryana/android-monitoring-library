package com.andresuryana.amlib.core.listener

interface OnSubscribeTopicListener {
    fun onReceive(topic: String, message: String)
    fun onCancel()
}