package com.andresuryana.amlib.core.di

import android.content.Context
import androidx.annotation.RestrictTo
import com.andresuryana.amlib.core.IMessagingClient
import com.andresuryana.amlib.core.di.factory.IMessagingClientFactory

/**
 * Provides singleton instances of core dependencies, ensuring that each dependency is created only once.
 *
 * The [SingletonProvider] class ensures that a single instance of [IMessagingClient] is created and reused throughout
 * the application's lifecycle. It uses double-checked locking to ensure thread-safe lazy initialization.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
object SingletonProvider {

    @Volatile
    private var messagingClient: IMessagingClient? = null

    /**
     * Retrieves the singleton instance of [IMessagingClient], creating it if necessary.
     *
     * This method initializes the [IMessagingClient] instance using [IMessagingClientFactory] if it has not already
     * been created. The initialization is thread-safe, ensuring that only one instance is created even in concurrent
     * access scenarios.
     *
     * @param context The [Context] used to retrieve application metadata for creating the [IMessagingClient] instance.
     * @return The singleton instance of [IMessagingClient].
     */
    fun getIMessagingClient(context: Context): IMessagingClient {
        return messagingClient ?: synchronized(this) {
            messagingClient ?: IMessagingClientFactory.create(context).also { messagingClient = it }
        }
    }
}