package com.andresuryana.amlib.logging

/**
 * Represents different levels of logging severity.
 *
 * @property priority The numeric priority associated with the log level, used to determine the importance or urgency of the log messages.
 */
enum class LogLevel(val priority: Int) {

    /**
     * Verbose level, typically used for detailed debugging information.
     * Priority: 2
     */
    VERBOSE(2),

    /**
     * Debug level, used for debugging information that is useful for diagnosing problems.
     * Priority: 3
     */
    DEBUG(3),

    /**
     * Info level, used for informational messages that highlight the progress of the application.
     * Priority: 4
     */
    INFO(4),

    /**
     * Warn level, used for warning messages that indicate potential issues or unusual conditions.
     * Priority: 5
     */
    WARN(5),

    /**
     * Error level, used for error messages that indicate significant problems or failures.
     * Priority: 6
     */
    ERROR(6),

    /**
     * Assert level, used for critical errors that indicate serious issues or faults.
     * Priority: 7
     */
    ASSERT(7);

    /**
     * Returns a short label representation of the log level.
     *
     * @return A single-character string representing the log level.
     */
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
        /**
         * Checks if the given log level is supported.
         *
         * @param level The short label of the log level to check.
         * @return `true` if the log level is supported; `false` otherwise.
         */
        fun isLogLevelSupported(level: String): Boolean {
            return entries.any { it.getShortLabel() == level }
        }

        /**
         * Retrieves the log level associated with the given priority.
         *
         * @param priority The numeric priority of the log level.
         * @return The corresponding [LogLevel] if found; `null` otherwise.
         */
        fun fromPriority(priority: Int): LogLevel? {
            return entries.firstOrNull { it.priority == priority }
        }
    }
}