package com.enmanuelgil.optimizer.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.PowerManager
import com.enmanuelgil.optimizer.model.DeviceStats
import com.enmanuelgil.optimizer.model.ThermalStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

class SystemMonitor(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var lastCpuIdle = 0L
    private var lastCpuTotal = 0L

    suspend fun getStats(): DeviceStats = withContext(Dispatchers.IO) {
        val ramInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(ramInfo)

        val ramTotalMb = ramInfo.totalMem / 1024 / 1024
        val ramAvailMb = ramInfo.availMem / 1024 / 1024
        val ramUsedMb = ramTotalMb - ramAvailMb

        val (swapUsed, swapTotal) = readSwapInfo()
        val (cpuTemp, batteryTemp, skinTemp) = readTemperatures()
        val thermalStatus = getThermalStatus(cpuTemp, skinTemp)
        val batteryInfo = getBatteryInfo()
        val storage = getStorageInfo()
        val cpuUsage = getCpuUsage()
        val processCount = activityManager.runningAppProcesses?.size ?: 0

        DeviceStats(
            cpuUsagePercent = cpuUsage,
            ramUsedMb = ramUsedMb,
            ramTotalMb = ramTotalMb,
            ramAvailableMb = ramAvailMb,
            swapUsedMb = swapUsed,
            swapTotalMb = swapTotal,
            temperatureCpu = cpuTemp,
            temperatureBattery = batteryTemp,
            temperatureSkin = skinTemp,
            batteryLevel = batteryInfo.first,
            batteryCharging = batteryInfo.second,
            thermalStatus = thermalStatus,
            runningProcesses = processCount,
            storageUsedGb = storage.first,
            storageTotalGb = storage.second
        )
    }

    private fun getCpuUsage(): Float {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val line = reader.readLine()
            reader.close()
            val parts = line.split(" ").filter { it.isNotEmpty() }
            if (parts.size < 8) return 0f
            val user = parts[1].toLong()
            val nice = parts[2].toLong()
            val system = parts[3].toLong()
            val idle = parts[4].toLong()
            val iowait = parts[5].toLong()
            val irq = parts[6].toLong()
            val softirq = parts[7].toLong()

            val total = user + nice + system + idle + iowait + irq + softirq
            val totalDiff = total - lastCpuTotal
            val idleDiff = idle - lastCpuIdle

            lastCpuTotal = total
            lastCpuIdle = idle

            if (totalDiff == 0L) 0f
            else ((totalDiff - idleDiff).toFloat() / totalDiff) * 100f
        } catch (e: Exception) { 0f }
    }

    private fun readSwapInfo(): Pair<Long, Long> {
        return try {
            var swapTotal = 0L
            var swapFree = 0L
            File("/proc/meminfo").forEachLine { line ->
                when {
                    line.startsWith("SwapTotal:") -> swapTotal = line.split("\\s+".toRegex())[1].toLong() / 1024
                    line.startsWith("SwapFree:") -> swapFree = line.split("\\s+".toRegex())[1].toLong() / 1024
                }
            }
            Pair(swapTotal - swapFree, swapTotal)
        } catch (e: Exception) { Pair(0L, 0L) }
    }

    private fun readTemperatures(): Triple<Float, Float, Float> {
        var cpuTemp = 0f
        var batteryTemp = 0f
        var skinTemp = 0f

        // Temperatura via ThermalManager (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val thermalManager = context.getSystemService(android.os.HardwarePropertiesManager::class.java)
                // Fallback a /sys/class/thermal
            } catch (e: Exception) {}
        }

        // Temperatura CPU via /sys/class/thermal
        try {
            val thermalDir = File("/sys/class/thermal/")
            thermalDir.listFiles()?.forEach { zone ->
                val typeFile = File(zone, "type")
                val tempFile = File(zone, "temp")
                if (typeFile.exists() && tempFile.exists()) {
                    val type = typeFile.readText().trim().lowercase()
                    val rawTemp = tempFile.readText().trim().toLongOrNull() ?: 0L
                    val temp = if (rawTemp > 1000) rawTemp / 1000f else rawTemp.toFloat()
                    when {
                        type.contains("cpu") || type.contains("ap") || type.contains("soc") ->
                            if (temp > cpuTemp) cpuTemp = temp
                        type.contains("skin") || type.contains("surface") ->
                            if (temp > skinTemp) skinTemp = temp
                        type.contains("bat") ->
                            if (batteryTemp == 0f) batteryTemp = temp
                    }
                }
            }
        } catch (e: Exception) {}

        // Temperatura batería via BatteryManager (más confiable)
        try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val rawBat = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            batteryTemp = rawBat / 10f
        } catch (e: Exception) {}

        return Triple(cpuTemp, batteryTemp, skinTemp)
    }

    private fun getThermalStatus(cpuTemp: Float, skinTemp: Float): ThermalStatus {
        // Usar ThermalManager nativo si disponible (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> ThermalStatus.NONE
                PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.LIGHT
                PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.MODERATE
                PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.SEVERE
                PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.CRITICAL
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.EMERGENCY
                else -> ThermalStatus.NONE
            }
        }
        // Fallback por temperatura
        val maxTemp = maxOf(cpuTemp, skinTemp)
        return when {
            maxTemp >= 55f -> ThermalStatus.CRITICAL
            maxTemp >= 50f -> ThermalStatus.SEVERE
            maxTemp >= 45f -> ThermalStatus.MODERATE
            maxTemp >= 40f -> ThermalStatus.LIGHT
            else -> ThermalStatus.NONE
        }
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            Pair(level, charging)
        } catch (e: Exception) { Pair(0, false) }
    }

    private fun getStorageInfo(): Pair<Float, Float> {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes / 1024f / 1024f / 1024f
            val free = stat.availableBytes / 1024f / 1024f / 1024f
            Pair(total - free, total)
        } catch (e: Exception) { Pair(0f, 0f) }
    }
}
