package com.andresuryana.amlib.logging.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * A class for managing logging-related preferences using [SharedPreferences].
 * This class provides methods to read and write preferences related to logging configuration.
 *
 * @param context The context used to access the [SharedPreferences] instance.
 */
class LoggingSharedPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * The device ID used for identifying the device in log messages.
     *
     * @return The device ID if set, otherwise `null`.
     */
    var deviceId: String?
        get() = prefs.getString(KEY_DEVICE_ID, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    /**
     * The priority of the log level used to filter log messages.
     *
     * @return The current log level priority. Defaults to -1 if not set.
     */
    var logLevelPriority: Int
        get() = prefs.getInt(KEY_LOG_LEVEL, -1)
        set(value) = prefs.edit().putInt(KEY_LOG_LEVEL, value).apply()

    /**
     * Whether console logging is enabled.
     *
     * @return `true` if console logging is enabled; `false` otherwise.
     */
    var isConsoleLogging: Boolean
        get() = prefs.getBoolean(KEY_CONSOLE_LOGGING, false)
        set(value) = prefs.edit().putBoolean(KEY_CONSOLE_LOGGING, value).apply()

    companion object {
        const val PREFS_NAME = "monitoring_lib_prefs"

        const val KEY_DEVICE_ID = "device_id"
        const val KEY_LOG_LEVEL = "log_level"
        const val KEY_CONSOLE_LOGGING = "console_logging"

        @Volatile
        private var INSTANCE: LoggingSharedPreferences? = null

        /**
         * Returns a singleton instance of [LoggingSharedPreferences].
         *
         * @param context The context used to create the [LoggingSharedPreferences] instance if it does not exist.
         * @return The singleton [LoggingSharedPreferences] instance.
         */
        fun getInstance(context: Context): LoggingSharedPreferences =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LoggingSharedPreferences(context).also { INSTANCE = it }
            }
    }
}