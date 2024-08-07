package com.andresuryana.amlib.core.di

import android.content.Context
import android.content.pm.PackageManager
import com.andresuryana.amlib.core.IMessagingClient
import com.andresuryana.amlib.core.client.mqtt.RabbitMQClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideIMessagingClient(@ApplicationContext context: Context): IMessagingClient {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        val metaData = appInfo.metaData

        val host = metaData.getString("com.andresuryana.amlib.MQTT_HOST")
        val port = metaData.getInt("com.andresuryana.amlib.MQTT_PORT")
        val username = metaData.getString("com.andresuryana.amlib.MQTT_USER")
        val password = metaData.getString("com.andresuryana.amlib.MQTT_PASS")
        val exchange = metaData.getString("com.andresuryana.amlib.MQTT_EXCHANGE")

        return RabbitMQClient(host, port, username, password, exchange)
    }
}