package com.andresuryana.amlib.core.di

import android.content.Context
import com.andresuryana.amlib.core.IMessagingClient

/**
 * Manages the creation and provision of core dependencies, ensuring that they are singleton instances.
 *
 * This class provides a way to access a singleton instance of [IMessagingClient] and ensures
 * that the instance is created only once throughout the application's lifecycle.
 *
 * @constructor Private constructor that initializes [CoreDependencies] with the given [Context].
 *
 * @param context The [Context] used to access application metadata for creating dependencies.
 */
class CoreDependencies private constructor(context: Context) {

    companion object {
        @Volatile
        private var instance: CoreDependencies? = null

        /**
         * Retrieves the singleton instance of [CoreDependencies], creating it if necessary.
         *
         * @param context The [Context] used to initialize the [CoreDependencies] instance.
         * @return The singleton instance of [CoreDependencies].
         */
        fun getInstance(context: Context): CoreDependencies {
            return instance ?: synchronized(this) {
                instance ?: CoreDependencies(context).also { instance = it }
            }
        }
    }

    private val messagingClient: IMessagingClient = SingletonProvider.getIMessagingClient(context)

    /**
     * Provides the singleton instance of [IMessagingClient].
     * This method returns the singleton instance of [IMessagingClient] that is managed by [SingletonProvider].
     *
     * @return The singleton instance of [IMessagingClient].
     */
    fun provideIMessagingClient(): IMessagingClient = messagingClient
}