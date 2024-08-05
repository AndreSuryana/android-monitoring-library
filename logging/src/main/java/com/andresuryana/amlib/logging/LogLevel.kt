package com.andresuryana.amlib.logging

enum class LogLevel(val priority: Int) {

    VERBOSE(2),
    DEBUG(3),
    INFO(4),
    WARN(5),
    ERROR(6),
    ASSERT(7);

    fun getShortLabel(): String {
        return when (this) {
            VERBOSE -> "V"
            DEBUG -> "D"
            INFO -> "I"
            WARN -> "W"
            ERROR -> "E"
            ASSERT -> "A"
        }
    }

    companion object {
        fun isLogLevelSupported(level: String): Boolean {
            return entries.any { it.getShortLabel() == level }
        }

        fun fromPriority(priority: Int): LogLevel? {
            return entries.firstOrNull { it.priority == priority }
        }
    }
}