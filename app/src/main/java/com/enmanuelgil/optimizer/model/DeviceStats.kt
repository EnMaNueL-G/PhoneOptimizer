package com.enmanuelgil.optimizer.model

data class DeviceStats(
    /** -1 = el sistema no deja leerlo (normal en Android 8+). */
    val cpuUsagePercent: Float = -1f,
    val ramUsedMb: Long = 0L,
    val ramTotalMb: Long = 0L,
    val ramAvailableMb: Long = 0L,
    val swapUsedMb: Long = 0L,
    val swapTotalMb: Long = 0L,
    /** 0 = no disponible en este teléfono. */
    val temperatureCpu: Float = 0f,
    val temperatureBattery: Float = 0f,
    val temperatureSkin: Float = 0f,
    val batteryLevel: Int = 0,
    val batteryCharging: Boolean = false,
    val batteryHealth: BatteryHealth = BatteryHealth.UNKNOWN,
    val thermalStatus: ThermalStatus = ThermalStatus.NONE,
    val storageUsedGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    /** Horas desde el último reinicio. */
    val uptimeHours: Long = 0L
) {
    val ramUsagePercent: Float
        get() = if (ramTotalMb > 0) (ramUsedMb.toFloat() / ramTotalMb) * 100f else 0f

    val storageUsagePercent: Float
        get() = if (storageTotalGb > 0) (storageUsedGb / storageTotalGb) * 100f else 0f

    val cpuAvailable: Boolean get() = cpuUsagePercent >= 0f
}

enum class ThermalStatus(val label: String, val color: Long) {
    NONE("Normal", 0xFF4CAF50),
    LIGHT("Leve", 0xFFFFEB3B),
    MODERATE("Moderado", 0xFFFF9800),
    SEVERE("Alto", 0xFFF44336),
    CRITICAL("Crítico", 0xFFE040FB),
    EMERGENCY("Emergencia", 0xFFFF4081)
}

enum class BatteryHealth(val label: String, val bad: Boolean) {
    UNKNOWN("Desconocida", false),
    GOOD("Buena", false),
    OVERHEAT("Sobrecalentada", true),
    DEAD("Agotada", true),
    OVER_VOLTAGE("Sobretensión", true),
    FAILURE("Fallo", true),
    COLD("Fría", false)
}

data class OptimizationResult(
    val actionsTaken: List<String> = emptyList(),
    val success: Boolean = true,
    val errorMessage: String? = null
)
