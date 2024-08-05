package com.andresuryana.amlib.deviceinfo.collector

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.annotation.RequiresApi
import com.andresuryana.amlib.common.IDeviceInfoCollector
import com.andresuryana.amlib.common.data.ResourceUsage
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader

class DefaultDeviceInfoCollector(private val context: Context) : IDeviceInfoCollector {

    private val bm: BatteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }

    override fun collectBatteryPercentage(): ResourceUsage<Int> {
        return try {
            ResourceUsage(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY), 100)
        } catch (e: Exception) {
            ResourceUsage(-1, 100) // Return -1 as error retrieving battery percentage
        }
    }

    override fun collectCpuUsage(): ResourceUsage<Float> {
        try {
            // Execute the top command
            val process = Runtime.getRuntime().exec("top -n 1")
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            var cpuUsage: Float? = null

            // Get the number of available processors
            val availableProcessors = Runtime.getRuntime().availableProcessors()

            // Parse the output
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("cpu", ignoreCase = true) == true) {
                    // Extract overall CPU usage from the line
                    line?.split("\\s+".toRegex())?.let { parts ->
                        val userCpuIndex = parts.indexOfFirst { it.contains("%user", ignoreCase = true) } - 1
                        val systemCpuIndex = parts.indexOfFirst { it.contains("%sys", ignoreCase = true) } - 1
                        val niceCpuIndex = parts.indexOfFirst { it.contains("%nice", ignoreCase = true) } - 1

                        val userCpu = if (userCpuIndex >= 0) parts[userCpuIndex].removeSuffix("%").toFloatOrNull() else 0f
                        val systemCpu = if (systemCpuIndex >= 0) parts[systemCpuIndex].removeSuffix("%").toFloatOrNull() else 0f
                        val niceCpu = if (niceCpuIndex >= 0) parts[niceCpuIndex].removeSuffix("%").toFloatOrNull() else 0f

                        val usedCpu = (userCpu ?: 0f) + (systemCpu ?: 0f) + (niceCpu ?: 0f)
                        val totalCapacity = availableProcessors * 100f // Calculate total capacity

                        // Calculate the total used CPU percentage
                        cpuUsage = usedCpu * 100 / totalCapacity
                    }
                    break
                }
            }

            // Return the overall CPU usage as a percentage
            cpuUsage?.let { usage ->
                return ResourceUsage(usage, 100f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ResourceUsage(-1f, 100f)
    }

    override fun collectMemoryUsage(): ResourceUsage<Float> {
        try {
            // Execute the top command
            val process = Runtime.getRuntime().exec("top -n 1")
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            var totalMemory: Float? = null
            var usedMemory: Float? = null

            // Parse the output
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("Mem:", ignoreCase = true) == true) {
                    // Extract memory information from the line
                    line?.split("\\s+".toRegex())?.let { parts ->
                        val totalMemoryIndex = parts.indexOfFirst { it.contains("total", ignoreCase = true) } - 1
                        val usedMemoryIndex = parts.indexOfFirst { it.contains("used", ignoreCase = true) } - 1

                        totalMemory = if (totalMemoryIndex >= 0) parseMemory(parts[totalMemoryIndex]) else null
                        usedMemory = if (usedMemoryIndex >= 0) parseMemory(parts[usedMemoryIndex]) else null
                    }
                    break
                }
            }

            // Return the memory usage as a percentage
            if (totalMemory != null && usedMemory != null) {
                val usedMemoryPercentage = (usedMemory!! / totalMemory!!) * 100
                return ResourceUsage(
                    used = usedMemoryPercentage,
                    total = 100f // Overall usage as a percentage
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ResourceUsage(-1f, 100f)
    }

    override fun collectStorageUsage(): ResourceUsage<Float> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android API 26 and above
            getStorageUsageApi26(context)
        } else {
            // For Android API below 26
            getStorageUsageBelowApi26()
        }
    }

    override fun collectTemperature(): Float {
        val temperaturePaths = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/hwmon/hwmon0/temp1_input"
        )

        for (path in temperaturePaths) {
            try {
                val file = File(path)
                if (file.exists()) {
                    val bufferedReader = BufferedReader(FileReader(file))
                    val temperature = bufferedReader.use { it.readLine().toFloatOrNull() }
                    if (temperature != null) {
                        return temperature / 1000 // Convert from milli-degrees to degrees if necessary
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return -1f
    }

    // Helper function to parse memory size strings (e.g., "1.8G", "212M")
    private fun parseMemory(memoryStr: String): Float {
        val unit = memoryStr.last()
        val value = memoryStr.dropLast(1).toFloatOrNull() ?: return 0f
        return when (unit) {
            'G' -> value * 1024 * 1024
            'M' -> value * 1024
            'K' -> value
            else -> value
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getStorageUsageApi26(context: Context): ResourceUsage<Float> {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageVolumes = storageManager.storageVolumes

        val primaryVolume = storageVolumes.find { it.isPrimary }
        val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For API 30 and above, use the new method
            primaryVolume?.directory?.path
        } else {
            // For API 26 to 29, use Environment.getExternalStorageDirectory()
            Environment.getExternalStorageDirectory().path
        }

        val statFs = StatFs(path)
        val blockSize = statFs.blockSizeLong
        val totalBlocks = statFs.blockCountLong
        val availableBlocks = statFs.availableBlocksLong

        val totalStorage = blockSize * totalBlocks
        val availableStorage = blockSize * availableBlocks
        val usedStorage = totalStorage - availableStorage

        return ResourceUsage(usedStorage.fromBytesToMB(), totalStorage.fromBytesToMB())
    }

    private fun getStorageUsageBelowApi26(): ResourceUsage<Float> {
        val statFs = StatFs(Environment.getExternalStorageDirectory().path)
        val blockSize = statFs.blockSizeLong
        val totalBlocks = statFs.blockCountLong
        val availableBlocks = statFs.availableBlocksLong

        val totalStorage = blockSize * totalBlocks
        val availableStorage = blockSize * availableBlocks
        val usedStorage = totalStorage - availableStorage

        return ResourceUsage(usedStorage.fromBytesToMB(), totalStorage.fromBytesToMB())
    }

    // Extension function to convert bytes to megabytes
    private fun Long.fromBytesToMB(): Float {
        return (this / 1024 / 1024).toFloat()
    }
}