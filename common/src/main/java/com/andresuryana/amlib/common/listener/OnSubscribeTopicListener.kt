package com.andresuryana.amlib.common.listener

interface OnSubscribeTopicListener {
    fun onReceive(topic: String, message: String)
    fun onCancel()
}