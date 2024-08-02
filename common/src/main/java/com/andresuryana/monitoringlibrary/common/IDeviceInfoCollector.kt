package com.andresuryana.monitoringlibrary.common

import com.andresuryana.monitoringlibrary.common.data.ResourceUsage

interface IDeviceInfoCollector {
    fun collectBatteryPercentage(): ResourceUsage<Int>
    fun collectCpuUsage(): ResourceUsage<Float>
    fun collectMemoryUsage(): ResourceUsage<Float>
    fun collectStorageUsage(): ResourceUsage<Float>
    fun collectTemperature(): Float
}