package com.andresuryana.monitoringlibrary.common.listener

interface OnSubscribeTopicListener {
    fun onReceive(topic: String, message: String)
    fun onCancel()
}