package com.andresuryana.amlib.core

import com.andresuryana.amlib.core.data.ResourceUsage

interface IDeviceInfoCollector {
    fun collectBatteryPercentage(): ResourceUsage<Int>
    fun collectCpuUsage(): ResourceUsage<Float>
    fun collectMemoryUsage(): ResourceUsage<Float>
    fun collectStorageUsage(): ResourceUsage<Float>
    fun collectTemperature(): Float
}