package com.enmanuelgil.optimizer.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.enmanuelgil.optimizer.model.OptimizationProfile
import com.enmanuelgil.optimizer.model.OptimizationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Aplica un perfil de ajustes (requiere el permiso concedido por ADB).
 * Cada cambio se relee para confirmar que quedó puesto, y el valor original se guarda para
 * poder restaurarlo. El resultado solo dice lo que se comprobó.
 */
class OptimizationEngine(private val context: Context) {

    companion object {
        /** Cerrar procesos de otras apps solo es posible hasta Android 13 (API 33). */
        val canCloseBackgroundApps: Boolean get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU
    }

    suspend fun optimize(
        profile: OptimizationProfile,
        onProgress: (String) -> Unit = {}
    ): OptimizationResult = withContext(Dispatchers.IO) {
        if (!PrivilegedHelper.hasWriteSecureSettings(context)) {
            return@withContext OptimizationResult(
                success = false,
                errorMessage = "Falta activar el modo avanzado (ver Ajustes)."
            )
        }
        val actions = mutableListOf<String>()

        onProgress("Ajustando animaciones...")
        val scale = profile.animationScale
        val animOk = applyAnimationScale(scale)
        actions += when {
            !animOk -> "✗ Animaciones: el sistema no aceptó el cambio"
            scale == 0f -> "Animaciones desactivadas"
            else -> "Animaciones al doble de rápido (0.5x)"
        }

        if (profile.disableWifiScan) {
            onProgress("Ajustando búsqueda WiFi/Bluetooth...")
            val ok1 = PrivilegedHelper.putGlobalVerified(context, "wifi_scan_always_enabled", "0")
            val ok2 = PrivilegedHelper.putGlobalVerified(context, "ble_scan_always_enabled", "0")
            actions += if (ok1 && ok2)
                "Sin búsquedas WiFi/Bluetooth con el WiFi apagado"
            else
                "✗ Búsqueda WiFi/Bluetooth: el sistema no aceptó el cambio"
        }

        // Limpiar la caché propia (Android no deja tocar la de otras apps)
        try {
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
        } catch (_: Exception) {}

        OptimizationResult(
            actionsTaken = actions,
            success = actions.none { it.startsWith("✗") },
            errorMessage = if (actions.any { it.startsWith("✗") }) "Algún ajuste no se pudo aplicar" else null
        )
    }

    /**
     * Pide a Android cerrar los procesos EN SEGUNDO PLANO de tus apps instaladas.
     * Solo existe en Android 13 o anterior: desde Android 14 el sistema lo ignora para
     * cualquier app que no sea la propia (documentación oficial de Android 14).
     * Aun en Android 8-13 algunos fabricantes lo ignoran, por eso se informa como "pedido",
     * no como "cerrado". Nunca afecta a la app en pantalla ni a la música que suena.
     */
    suspend fun requestCloseBackgroundApps(): Int = withContext(Dispatchers.IO) {
        if (!canCloseBackgroundApps) return@withContext 0
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pkgs = try {
            context.packageManager.getInstalledApplications(0)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 && it.packageName != context.packageName }
                .map { it.packageName }
        } catch (_: Exception) { emptyList() }
        var n = 0
        pkgs.forEach { p -> try { am.killBackgroundProcesses(p); n++ } catch (_: Exception) {} }
        n
    }

    private fun applyAnimationScale(scale: Float): Boolean {
        val v = if (scale == 0f) "0" else scale.toString()
        return listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
            .map { PrivilegedHelper.putGlobalVerified(context, it, v) }
            .all { it }
    }
}
