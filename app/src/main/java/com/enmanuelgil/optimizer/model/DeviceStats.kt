package com.enmanuelgil.optimizer.model

data class DeviceStats(
    val cpuUsagePercent: Float = 0f,
    val ramUsedMb: Long = 0L,
    val ramTotalMb: Long = 0L,
    val ramAvailableMb: Long = 0L,
    val swapUsedMb: Long = 0L,
    val swapTotalMb: Long = 0L,
    val temperatureCpu: Float = 0f,
    val temperatureBattery: Float = 0f,
    val temperatureSkin: Float = 0f,
    val batteryLevel: Int = 0,
    val batteryCharging: Boolean = false,
    val thermalStatus: ThermalStatus = ThermalStatus.NONE,
    val runningProcesses: Int = 0,
    val storageUsedGb: Float = 0f,
    val storageTotalGb: Float = 0f
) {
    val ramUsagePercent: Float
        get() = if (ramTotalMb > 0) (ramUsedMb.toFloat() / ramTotalMb) * 100f else 0f

    val storageUsagePercent: Float
        get() = if (storageTotalGb > 0) (storageUsedGb / storageTotalGb) * 100f else 0f
}

enum class ThermalStatus(val label: String, val color: Long) {
    NONE("Normal", 0xFF4CAF50),
    LIGHT("Tibio", 0xFFFFEB3B),
    MODERATE("Caliente", 0xFFFF9800),
    SEVERE("Crítico", 0xFFF44336),
    CRITICAL("Emergencia", 0xFF9C27B0),
    EMERGENCY("Apagando", 0xFF000000)
}

data class OptimizationResult(
    val actionsTaken: List<String> = emptyList(),
    val ramFreedMb: Long = 0L,
    val cacheFreedMb: Long = 0L,
    val appsKilled: Int = 0,
    val temperatureBefore: Float = 0f,
    val temperatureAfter: Float = 0f,
    val success: Boolean = true,
    val errorMessage: String? = null
)
