package com.andresuryana.amlib

import android.app.Application
import android.util.Log
import com.andresuryana.amlib.logging.AppLogger
import com.andresuryana.amlib.logging.LogLevel

class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize device ID for LoggingService
        AppLogger.setDeviceId("1234567890")
        AppLogger.setLogLevel(LogLevel.DEBUG)
        AppLogger.setConsoleLogging(true)

        // Check device ID successfully stored
        Log.d(TAG, "onCreate: Device ID: ${AppLogger.getDeviceId()}")
    }

    companion object {
        const val TAG: String = "BaseApplication"
    }
}