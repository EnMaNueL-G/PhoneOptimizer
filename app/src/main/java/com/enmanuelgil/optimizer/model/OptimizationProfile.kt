package com.enmanuelgil.optimizer.model

enum class OptimizationProfile(
    val displayName: String,
    val description: String,
    val killAggressiveness: KillLevel,
    val reducedAnimations: Boolean,
    val disableNfc: Boolean,
    val disableWifiScan: Boolean,
    val restrictBackgroundData: Boolean,
    val pauseSync: Boolean,
    val enableDoze: Boolean,
    val thermalAlertTemp: Float,
    val autoOptimizeOnHeat: Boolean
) {
    RECOMMENDED(
        displayName = "Recomendado",
        description = "Balance óptimo entre rendimiento y funcionalidad",
        killAggressiveness = KillLevel.MODERATE,
        reducedAnimations = true,
        disableNfc = false,
        disableWifiScan = true,
        restrictBackgroundData = true,
        pauseSync = false,
        enableDoze = true,
        thermalAlertTemp = 45f,
        autoOptimizeOnHeat = true
    ),
    PERFORMANCE(
        displayName = "Rendimiento",
        description = "Máxima fluidez — más consumo de batería",
        killAggressiveness = KillLevel.AGGRESSIVE,
        reducedAnimations = true,
        disableNfc = true,
        disableWifiScan = true,
        restrictBackgroundData = true,
        pauseSync = true,
        enableDoze = false,
        thermalAlertTemp = 50f,
        autoOptimizeOnHeat = true
    ),
    BATTERY_SAVER(
        displayName = "Ahorro de Batería",
        description = "Máxima duración de batería",
        killAggressiveness = KillLevel.AGGRESSIVE,
        reducedAnimations = true,
        disableNfc = true,
        disableWifiScan = true,
        restrictBackgroundData = true,
        pauseSync = true,
        enableDoze = true,
        thermalAlertTemp = 40f,
        autoOptimizeOnHeat = true
    ),
    THERMAL_GUARD(
        displayName = "Protección Térmica",
        description = "Prioriza enfriar el dispositivo",
        killAggressiveness = KillLevel.MAXIMUM,
        reducedAnimations = true,
        disableNfc = true,
        disableWifiScan = true,
        restrictBackgroundData = true,
        pauseSync = true,
        enableDoze = true,
        thermalAlertTemp = 38f,
        autoOptimizeOnHeat = true
    ),
    CUSTOM(
        displayName = "Personalizado",
        description = "Configura cada ajuste manualmente",
        killAggressiveness = KillLevel.MODERATE,
        reducedAnimations = false,
        disableNfc = false,
        disableWifiScan = false,
        restrictBackgroundData = false,
        pauseSync = false,
        enableDoze = false,
        thermalAlertTemp = 50f,
        autoOptimizeOnHeat = false
    )
}

enum class KillLevel { LIGHT, MODERATE, AGGRESSIVE, MAXIMUM }
