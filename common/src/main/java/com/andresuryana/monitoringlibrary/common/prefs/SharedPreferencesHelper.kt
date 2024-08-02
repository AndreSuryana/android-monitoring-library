package com.andresuryana.monitoringlibrary.common.prefs

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesHelper private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var deviceId: String?
        get() = prefs.getString(KEY_DEVICE_ID, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var logLevelPriority: Int
        get() = prefs.getInt(KEY_LOG_LEVEL, -1)
        set(value) = prefs.edit().putInt(KEY_LOG_LEVEL, value).apply()

    var isConsoleLogging: Boolean
        get() = prefs.getBoolean(KEY_CONSOLE_LOGGING, false)
        set(value) = prefs.edit().putBoolean(KEY_CONSOLE_LOGGING, value).apply()


    companion object {
        const val PREFS_NAME = "monitoring_lib_prefs"

        const val KEY_DEVICE_ID = "device_id"
        const val KEY_LOG_LEVEL = "log_level"
        const val KEY_CONSOLE_LOGGING = "console_logging"

        @Volatile
        private var INSTANCE: SharedPreferencesHelper? = null

        fun getInstance(context: Context): SharedPreferencesHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SharedPreferencesHelper(context).also { INSTANCE = it }
            }
    }
}