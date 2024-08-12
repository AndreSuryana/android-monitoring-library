package com.andresuryana.amlib.logging

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * A ContentProvider implementation that initializes the application logger when the provider is created.
 *
 * This provider serves as an entry point for initializing the logging system within the application. It is
 * designed to be registered in the application's manifest to ensure that logging is set up as soon as the application
 * starts. The provider performs the following tasks:
 *
 * - **Initialization**: When the provider is created, it initializes the {@link AppLogger} with the application context.
 *
 * The other methods (query, getType, insert, delete, update) are not implemented and return default values. They
 * can be overridden if needed for specific use cases.
 */
class ContextProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        context?.let {
            AppLogger.initialize(it)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}