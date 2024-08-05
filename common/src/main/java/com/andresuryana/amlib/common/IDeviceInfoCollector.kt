package com.andresuryana.amlib.common

import com.andresuryana.amlib.common.data.ResourceUsage

interface IDeviceInfoCollector {
    fun collectBatteryPercentage(): ResourceUsage<Int>
    fun collectCpuUsage(): ResourceUsage<Float>
    fun collectMemoryUsage(): ResourceUsage<Float>
    fun collectStorageUsage(): ResourceUsage<Float>
    fun collectTemperature(): Float
}