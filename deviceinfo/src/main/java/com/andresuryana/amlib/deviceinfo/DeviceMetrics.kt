package com.andresuryana.amlib.deviceinfo

import com.andresuryana.amlib.common.data.ResourceUsage

data class DeviceMetrics(
    val batteryPercentage: ResourceUsage<Int>,
    val cpuUsage: ResourceUsage<Float>,
    val memoryUsage: ResourceUsage<Float>,
    val storageUsage: ResourceUsage<Float>,
    val temperature: Float
)