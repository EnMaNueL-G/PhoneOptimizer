package com.enmanuelgil.optimizer.model

/**
 * Perfiles de ajuste. Solo contienen cambios que Android permite de verdad (con el permiso
 * concedido por ADB) y que se pueden comprobar y restaurar.
 *
 * Ya no se "cierran apps en segundo plano": desde Android 14 el sistema no deja a ninguna app
 * cerrar procesos de otras (killBackgroundProcesses solo afecta a la propia app) y Google
 * desaconseja hacerlo también en versiones anteriores. Probado en un vivo con Android 13:
 * tampoco surtía efecto.
 */
enum class OptimizationProfile(
    val displayName: String,
    val description: String,
    /** Escala de animaciones (1.0 = normal, 0 = sin animaciones). */
    val animationScale: Float,
    /** Desactivar la búsqueda de redes WiFi/Bluetooth con el WiFi apagado. */
    val disableWifiScan: Boolean
) {
    RECOMMENDED(
        displayName = "Recomendado",
        description = "Animaciones al doble de rápido: el teléfono se siente más ágil",
        animationScale = 0.5f,
        disableWifiScan = false
    ),
    PERFORMANCE(
        displayName = "Máxima agilidad",
        description = "Sin animaciones: todo aparece al instante (menos vistoso)",
        animationScale = 0f,
        disableWifiScan = false
    ),
    BATTERY_SAVER(
        displayName = "Ahorro de batería",
        description = "Animaciones rápidas y sin búsquedas WiFi/Bluetooth con el WiFi apagado " +
            "(la ubicación puede ser algo menos precisa)",
        animationScale = 0.5f,
        disableWifiScan = true
    )
}
