package com.andresuryana.amlib.logging

import java.util.Date
import java.util.UUID

data class LogData(
    val uid: String,
    val timestamp: Date,
    val level: LogLevel,
    val tag: String,
    val message: String,
) {
    constructor(level: LogLevel, tag: String, message: String) : this(
        UUID.randomUUID().toString(),
        Date(),
        level,
        tag,
        message
    )
}
