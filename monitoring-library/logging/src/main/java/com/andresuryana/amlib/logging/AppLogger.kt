package com.andresuryana.amlib.logging

import android.content.Context
import android.content.Intent
import android.util.Log
import com.andresuryana.amlib.logging.prefs.LoggingSharedPreferences
import com.andresuryana.amlib.logging.service.LoggingService
import java.lang.ref.WeakReference

/**
 * A utility object for logging within the application. It provides methods for initializing the logger,
 * configuring logging preferences, and sending log messages of various levels (VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT).
 *
 * This object allows you to:
 * - Initialize the logger with the application context.
 * - Set and get logging preferences such as device ID, log level, and console logging.
 * - Log messages with different log levels and optionally attach stack traces for exceptions.
 * - Ensure that log messages are sent to a logging service if the application context is initialized.
 */
object AppLogger {

    @Volatile
    private var contextRef: WeakReference<Context>? = null

    /**
     * Initializes the logger with the given application context.
     *
     * @param context The application context to be used for logging operations.
     */
    fun initialize(context: Context) {
        contextRef = WeakReference(context.applicationContext)
    }

    /**
     * Sets the device ID for logging.
     *
     * @param deviceId The device ID to be set.
     */
    @JvmStatic
    fun setDeviceId(deviceId: String) {
        contextRef?.get()?.let { context ->
            val prefs = LoggingSharedPreferences.getInstance(context)
            prefs.deviceId = deviceId
        }
    }

    /**
     * Retrieves the currently set device ID.
     *
     * @return The current device ID, or null if not set.
     */
    @JvmStatic
    fun getDeviceId(): String? {
        contextRef?.get()?.let { context ->
            val prefs = LoggingSharedPreferences.getInstance(context)
            return prefs.deviceId
        }
        return null
    }

    /**
     * Sets the log level for the logger.
     *
     * @param level The log level to be set.
     */
    @JvmStatic
    fun setLogLevel(level: LogLevel) {
        contextRef?.get()?.let { context ->
            val prefs = LoggingSharedPreferences.getInstance(context)
            prefs.logLevelPriority = level.priority
        }
    }

    /**
     * Retrieves the current log level.
     *
     * @return The current log level, or VERBOSE if not set.
     */
    @JvmStatic
    fun getLogLevel(): LogLevel? {
        contextRef?.get()?.let { context ->
            val prefs = LoggingSharedPreferences.getInstance(context)
            return LogLevel.fromPriority(prefs.logLevelPriority)
        }
        return LogLevel.VERBOSE
    }

    /**
     * Enables or disables console logging.
     *
     * @param enable True to enable console logging, false to disable.
     */
    @JvmStatic
    fun setConsoleLogging(enable: Boolean) {
        contextRef?.get()?.let { context ->
            val prefs = LoggingSharedPreferences.getInstance(context)
            prefs.isConsoleLogging = enable
        }
    }

    /**
     * Checks if console logging is enabled.
     *
     * @return True if console logging is enabled, false otherwise.
     */
    @JvmStatic
    fun isConsoleLogging(): Boolean {
        contextRef?.get()?.let { context ->
            val prefs = LoggingSharedPreferences.getInstance(context)
            return prefs.isConsoleLogging
        }
        return false
    }

    /**
     * Logs a VERBOSE message.
     *
     * @param tag The tag for the log message.
     * @param message The message to be logged.
     */
    @JvmStatic
    fun v(tag: String, message: String) {
        log(LogLevel.VERBOSE, tag, message)
    }

    /**
     * Logs a VERBOSE message with an exception.
     *
     * @param tag The tag for the log message.
     * @param message The message to be logged.
     * @param tr The exception to be logged.
     */
    @JvmStatic
    fun v(tag: String, message: String, tr: Throwable) {
        log(LogLevel.VERBOSE, tag, "$message\n${Log.getStackTraceString(tr)}")
    }

    /**
     * Logs a DEBUG message.
     *
     * @param tag The tag for the log message.
     * @param message The message to be logged.
     */
    @JvmStatic
    fun d(tag: String, message: String) {
        log(LogLevel.DEBUG, tag, message)
    }

    /**
     * Logs a DEBUG message with an exception.
     *
     * @param tag The tag for the log message.
     * @param message The message to be logged.
     * @param tr The exception to be logged.
     */
    @JvmStatic
    fun d(tag: String, message: String, tr: Throwable) {
        log(LogLevel.DEBUG, tag, "$message\n${Log.getStackTraceString(tr)}")
    }

    /**
     * Logs a INFO message.
     *
     * @param tag The tag for the log message.
     * @param message The message to be logged.
     */
    @JvmStatic
    fun i(tag: String, message: String) {
        log(LogLevel.INFO, tag, message)
    }

    /**
     * Logs a INFO message with an exception.
     *
     * @param tag The tag for the log message.
     * @param message The message to be logged.
     * @param tr The exception to be logged.
     */
    @JvmStatic
    fun i(tag: String, message: String, tr: Throwable) {
        log(LogLevel.INFO, tag, "$message\n${Log.getStackTraceString(tr)}")
    }

    /**
     * Logs a WARNING message.
     *
     * @param tag The tag for the log message.
     * @param message The message to be logged.
     */
    @JvmStatic
    fun w(tag: String, message: String) {
        log(LogLevel.WARN, tag, message)
    }

    /**
     * Logs a WARNING message with an exception.
     *
     * @param tag The tag for the log message.
     * @param message The message to be logged.
     * @param tr The exception to be logged.
     */
    @JvmStatic
    fun w(tag: String, message: String, tr: Throwable) {
        log(LogLevel.WARN, tag, "$message\n${Log.getStackTraceString(tr)}")
    }

    /**
     * Logs a WARNING message with an exception.
     *
     * @param tag The tag for the log message.
     * @param tr The exception to be logged.
     */
    @JvmStatic
    fun w(tag: String, tr: Throwable) {
        log(LogLevel.WARN, tag, Log.getStackTraceString(tr))
    }

    /**
     * Logs a ERROR message.
     *
     * @param tag The tag for the log message.
     * @param message The message to be logged.
     */
    @JvmStatic
    fun e(tag: String, message: String) {
        log(LogLevel.ERROR, tag, message)
    }

    /**
     * Logs a ERROR message with an exception.
     *
     * @param tag The tag for the log message.
     * @param message The message to be logged.
     * @param tr The exception to be logged.
     */
    @JvmStatic
    fun e(tag: String, message: String, tr: Throwable) {
        log(LogLevel.ERROR, tag, "$message\n${Log.getStackTraceString(tr)}")
    }

    /**
     * Logs a WTF (What a Terrible Failure) message.
     *
     * @param tag The tag for the log message.
     * @param message The message to be logged.
     */
    @JvmStatic
    fun wtf(tag: String, message: String) {
        log(LogLevel.ASSERT, tag, message)
    }

    /**
     * Logs a WTF (What a Terrible Failure) message.
     *
     * @param tag The tag for the log message.
     * @param tr The exception to be logged.
     */
    @JvmStatic
    fun wtf(tag: String, tr: Throwable) {
        log(LogLevel.ASSERT, tag, Log.getStackTraceString(tr))
    }

    /**
     * Logs a WTF (What a Terrible Failure) message.
     *
     * @param tag The tag for the log message.
     * @param message The message to be logged.
     * @param tr The exception to be logged.
     */
    @JvmStatic
    fun wtf(tag: String, message: String, tr: Throwable) {
        log(LogLevel.ASSERT, tag, "$message\n${Log.getStackTraceString(tr)}")
    }

    /**
     * Logs a message with the specified log level.
     *
     * @param level The log level.
     * @param tag The tag for the log message.
     * @param message The message to be logged.
     * @param tr Optional exception to be logged.
     */
    private fun log(level: LogLevel, tag: String, message: String, tr: Throwable? = null) {
        // Check context reference first
        val context = contextRef?.get()
            ?: throw IllegalStateException("Application context not initialized. Make sure '${ContextProvider::class.java.name}' is registered in your manifest as provider.")

        // Check if the log level is enabled
        if (!isLoggable(level)) return

        // Get and validate device ID
        val deviceId = getDeviceId()
            ?: throw IllegalStateException("Device ID not initialized. Please call setDeviceId() first.")

        // Log to console if enabled
        if (isConsoleLogging()) {
            when (level) {
                LogLevel.VERBOSE -> Log.v(tag, message, tr)
                LogLevel.DEBUG -> Log.d(tag, message, tr)
                LogLevel.INFO -> Log.i(tag, message, tr)
                LogLevel.WARN -> Log.w(tag, message, tr)
                LogLevel.ERROR -> Log.e(tag, message, tr)
                LogLevel.ASSERT -> Log.wtf(tag, message, tr)
            }
        }

        // Send log to service
        val intent = Intent(context, LoggingService::class.java).apply {
            putExtra(LoggingService.EXTRA_TAG, tag)
            putExtra(LoggingService.EXTRA_LEVEL, level.getShortLabel())
            putExtra(
                LoggingService.EXTRA_MESSAGE,
                if (tr != null) "$message\n${Log.getStackTraceString(tr)}" else message
            )
            putExtra(LoggingService.EXTRA_DEVICE_ID, deviceId)
        }
        context.startService(intent)
    }

    /**
     * Determines if the given log level should be logged based on the current log level setting.
     *
     * @param level The log level to check.
     * @return True if the level is loggable, false otherwise.
     */
    private fun isLoggable(level: LogLevel): Boolean {
        val logLevel = getLogLevel() ?: return false
        return level.priority >= logLevel.priority
    }
}