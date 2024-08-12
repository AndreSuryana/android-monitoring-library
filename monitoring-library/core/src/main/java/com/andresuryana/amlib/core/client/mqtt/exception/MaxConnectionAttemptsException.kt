package com.andresuryana.amlib.core.client.mqtt.exception

class MaxConnectionAttemptsException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)