package com.enmanuelgil.optimizer.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import com.enmanuelgil.optimizer.model.BatteryHealth
import com.enmanuelgil.optimizer.model.DeviceStats
import com.enmanuelgil.optimizer.model.ThermalStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lecturas del teléfono que Android permite a una app normal. Lo que el sistema no deja
 * leer se marca como "no disponible" en vez de inventar un valor.
 */
class SystemMonitor(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private var lastCpuIdle = 0L
    private var lastCpuTotal = 0L
    /** null = aún no se sabe; false = el sistema no deja leer /proc/stat (no se reintenta). */
    private var cpuReadable: Boolean? = null
    /** Zonas térmicas legibles de CPU (se buscan una vez). */
    private var cpuZones: List<File>? = null

    suspend fun getStats(): DeviceStats = withContext(Dispatchers.IO) {
        val ramInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(ramInfo)

        val ramTotalMb = ramInfo.totalMem / 1024 / 1024
        val ramAvailMb = ramInfo.availMem / 1024 / 1024

        val (swapUsed, swapTotal) = readSwapInfo()
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryTemp = (battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val cpuTemp = readCpuTemperature()
        val storage = getStorageInfo()

        DeviceStats(
            cpuUsagePercent = getCpuUsage(),
            ramUsedMb = ramTotalMb - ramAvailMb,
            ramTotalMb = ramTotalMb,
            ramAvailableMb = ramAvailMb,
            swapUsedMb = swapUsed,
            swapTotalMb = swapTotal,
            temperatureCpu = cpuTemp,
            temperatureBattery = batteryTemp,
            batteryLevel = batteryLevel(battery),
            batteryCharging = isCharging(battery),
            batteryHealth = batteryHealth(battery),
            thermalStatus = getThermalStatus(batteryTemp),
            storageUsedGb = storage.first,
            storageTotalGb = storage.second,
            uptimeHours = SystemClock.elapsedRealtime() / 3_600_000L
        )
    }

    // ── CPU ────────────────────────────────────────────────────────────────
    private fun readRawCpuStats(): Pair<Long, Long>? = try {
        val line = File("/proc/stat").useLines { l -> l.firstOrNull { it.startsWith("cpu ") } }
        val parts = line?.split(" ")?.filter { it.isNotEmpty() }
        if (parts == null || parts.size < 8) null else {
            val v = (1..7).map { parts[it].toLong() }
            Pair(v.sum(), v[3])  // total, idle
        }
    } catch (_: Exception) { null }

    /** Uso de CPU real desde /proc/stat, o -1 si el sistema no lo permite (Android 8+). */
    private fun getCpuUsage(): Float {
        if (cpuReadable == false) return -1f
        val raw = readRawCpuStats()
        if (raw == null) { cpuReadable = false; return -1f }
        val (total, idle) = raw
        val first = cpuReadable == null
        cpuReadable = true
        val totalDiff = total - lastCpuTotal
        val idleDiff = idle - lastCpuIdle
        lastCpuTotal = total
        lastCpuIdle = idle
        if (first || totalDiff <= 0) return -1f
        return ((totalDiff - idleDiff).toFloat() / totalDiff * 100f).coerceIn(0f, 100f)
    }

    // ── Temperatura ────────────────────────────────────────────────────────
    /** Temperatura de CPU si el fabricante deja leerla; 0 si no. */
    private fun readCpuTemperature(): Float {
        val zones = cpuZones ?: findCpuZones().also { cpuZones = it }
        var max = 0f
        for (tempFile in zones) {
            val raw = try { tempFile.readText().trim().toLongOrNull() } catch (_: Exception) { null } ?: continue
            val t = if (raw > 1000) raw / 1000f else raw.toFloat()
            if (t in 15f..120f && t > max) max = t
        }
        return max
    }

    private fun findCpuZones(): List<File> = try {
        File("/sys/class/thermal/").listFiles()
            ?.filter { it.name.startsWith("thermal_zone") }
            ?.mapNotNull { zone ->
                val type = try { File(zone, "type").readText().trim().lowercase() } catch (_: Exception) { return@mapNotNull null }
                val temp = File(zone, "temp")
                // OJO: "soc" a secas es el % de carga en muchos Qualcomm (no una temperatura)
                val isCpu = type.startsWith("cpu") || type.contains("cpu-") || type.contains("soc_thermal") ||
                    type.contains("tsens_tz_sensor") || type == "mtktscpu"
                if (isCpu && temp.canRead()) temp else null
            } ?: emptyList()
    } catch (_: Exception) { emptyList() }

    /**
     * Estado térmico: en Android 10+ lo da el propio sistema (fiable). En Android 8-9 se estima
     * con la temperatura de la batería (umbrales conservadores: 40 °C en batería ya es caliente).
     */
    private fun getThermalStatus(batteryTemp: Float): ThermalStatus {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.LIGHT
                PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.MODERATE
                PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.SEVERE
                PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.CRITICAL
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.EMERGENCY
                else -> ThermalStatus.NONE
            }
        }
        return when {
            batteryTemp >= 48f -> ThermalStatus.SEVERE
            batteryTemp >= 44f -> ThermalStatus.MODERATE
            batteryTemp >= 40f -> ThermalStatus.LIGHT
            else -> ThermalStatus.NONE
        }
    }

    // ── Batería ────────────────────────────────────────────────────────────
    private fun batteryLevel(intent: Intent?): Int {
        val cap = try { batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } catch (_: Exception) { -1 }
        if (cap in 0..100) return cap
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (level >= 0 && scale > 0) level * 100 / scale else 0
    }

    private fun isCharging(intent: Intent?): Boolean {
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun batteryHealth(intent: Intent?): BatteryHealth =
        when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.FAILURE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
            else -> BatteryHealth.UNKNOWN
        }

    // ── Memoria y almacenamiento ───────────────────────────────────────────
    private fun readSwapInfo(): Pair<Long, Long> = try {
        var swapTotal = 0L
        var swapFree = 0L
        File("/proc/meminfo").useLines { lines ->
            for (line in lines) {
                if (line.startsWith("SwapTotal:")) swapTotal = kb(line) / 1024
                else if (line.startsWith("SwapFree:")) { swapFree = kb(line) / 1024; break }
            }
        }
        Pair((swapTotal - swapFree).coerceAtLeast(0), swapTotal)
    } catch (_: Exception) { Pair(0L, 0L) }

    private fun kb(line: String): Long =
        line.substringAfter(':').trim().substringBefore(' ').toLongOrNull() ?: 0L

    private fun getStorageInfo(): Pair<Float, Float> = try {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes / 1024f / 1024f / 1024f
        val free = stat.availableBytes / 1024f / 1024f / 1024f
        Pair(total - free, total)
    } catch (_: Exception) { Pair(0f, 0f) }
}
