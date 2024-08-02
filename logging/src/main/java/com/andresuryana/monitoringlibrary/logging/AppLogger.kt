package com.andresuryana.monitoringlibrary.logging

import android.content.Context
import android.content.Intent
import android.util.Log
import com.andresuryana.monitoringlibrary.common.prefs.SharedPreferencesHelper
import com.andresuryana.monitoringlibrary.logging.service.LoggingService
import java.lang.ref.WeakReference

class AppLogger {

    companion object {

        @Volatile
        private var contextRef: WeakReference<Context>? = null

        fun initialize(context: Context) {
            contextRef = WeakReference(context.applicationContext)
        }

        fun setDeviceId(deviceId: String) {
            contextRef?.get()?.let { context ->
                val prefs = SharedPreferencesHelper.getInstance(context)
                prefs.deviceId = deviceId
            }
        }

        fun getDeviceId(): String? {
            contextRef?.get()?.let { context ->
                val prefs = SharedPreferencesHelper.getInstance(context)
                return prefs.deviceId
            }
            return null
        }

        fun setLogLevel(level: LogLevel) {
            contextRef?.get()?.let { context ->
                val prefs = SharedPreferencesHelper.getInstance(context)
                prefs.logLevelPriority = level.priority
            }
        }

        fun getLogLevel(): LogLevel? {
            contextRef?.get()?.let { context ->
                val prefs = SharedPreferencesHelper.getInstance(context)
                return LogLevel.fromPriority(prefs.logLevelPriority)
            }
            return LogLevel.VERBOSE
        }

        fun setConsoleLogging(enable: Boolean) {
            contextRef?.get()?.let { context ->
                val prefs = SharedPreferencesHelper.getInstance(context)
                prefs.isConsoleLogging = enable
            }
        }

        fun isConsoleLogging(): Boolean {
            contextRef?.get()?.let { context ->
                val prefs = SharedPreferencesHelper.getInstance(context)
                return prefs.isConsoleLogging
            }
            return false
        }

        fun v(tag: String, message: String) {
            log(LogLevel.VERBOSE, tag, message)
        }

        fun v(tag: String, message: String, tr: Throwable) {
            log(LogLevel.VERBOSE, tag, "$message\n${Log.getStackTraceString(tr)}")
        }

        fun d(tag: String, message: String) {
            log(LogLevel.DEBUG, tag, message)
        }

        fun d(tag: String, message: String, tr: Throwable) {
            log(LogLevel.DEBUG, tag, "$message\n${Log.getStackTraceString(tr)}")
        }

        fun i(tag: String, message: String) {
            log(LogLevel.INFO, tag, message)
        }

        fun i(tag: String, message: String, tr: Throwable) {
            log(LogLevel.INFO, tag, "$message\n${Log.getStackTraceString(tr)}")
        }

        fun w(tag: String, message: String) {
            log(LogLevel.WARN, tag, message)
        }

        fun w(tag: String, message: String, tr: Throwable) {
            log(LogLevel.WARN, tag, "$message\n${Log.getStackTraceString(tr)}")
        }

        fun w(tag: String, tr: Throwable) {
            log(LogLevel.WARN, tag, Log.getStackTraceString(tr))
        }

        fun e(tag: String, message: String) {
            log(LogLevel.ERROR, tag, message)
        }

        fun e(tag: String, message: String, tr: Throwable) {
            log(LogLevel.ERROR, tag, "$message\n${Log.getStackTraceString(tr)}")
        }

        fun wtf(tag: String, message: String) {
            log(LogLevel.ASSERT, tag, message)
        }

        fun wtf(tag: String, tr: Throwable) {
            log(LogLevel.ASSERT, tag, Log.getStackTraceString(tr))
        }

        fun wtf(tag: String, message: String, tr: Throwable) {
            log(LogLevel.ASSERT, tag, "$message\n${Log.getStackTraceString(tr)}")
        }

        private fun log(level: LogLevel, tag: String, message: String, tr: Throwable? = null) {
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
            val context = contextRef?.get()
                ?: throw IllegalStateException("Application context not initialized. Make sure LoggingService is registered in your manifest.")
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

        private fun isLoggable(level: LogLevel): Boolean {
            val logLevel = getLogLevel() ?: return false
            return level.priority >= logLevel.priority
        }
    }
}