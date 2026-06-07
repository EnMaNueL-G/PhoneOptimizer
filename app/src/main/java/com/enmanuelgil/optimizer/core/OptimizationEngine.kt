package com.enmanuelgil.optimizer.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import com.enmanuelgil.optimizer.model.KillLevel
import com.enmanuelgil.optimizer.model.OptimizationProfile
import com.enmanuelgil.optimizer.model.OptimizationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class OptimizationEngine(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val resolver = context.contentResolver

    private val systemProtected = setOf(
        "android", "com.android.systemui", "com.android.phone",
        "com.android.launcher", "com.miui.home", "com.samsung.android.launcher",
        "com.google.android.gms", "com.google.android.gsf",
        "com.android.settings", "com.android.inputmethod.latin",
        context.packageName
    )

    private val heavyApps = setOf(
        "com.facebook.katana", "com.facebook.lite", "com.facebook.orca",
        "com.facebook.mlite", "com.facebook.appmanager",
        "com.instagram.android", "com.zhiliaoapp.musically",
        "com.snapchat.android", "com.google.android.apps.photos",
        "com.android.vending", "com.netflix.mediaclient",
        "com.spotify.music", "com.dts.freefireth",
        "com.amazon.dee.app", "com.einnovation.temu",
        "com.microsoft.teams", "com.linkedin.android",
        "com.google.android.youtube", "com.twitter.android"
    )

    suspend fun optimize(
        profile: OptimizationProfile,
        temperatureBefore: Float = 0f,
        onProgress: (String) -> Unit = {}
    ): OptimizationResult = withContext(Dispatchers.IO) {
        val actions = mutableListOf<String>()
        var appsKilled = 0
        val ramBefore = getFreeRamMb()
        val hasPrivileges = PrivilegedHelper.hasWriteSecureSettings(context)

        // 1. Liberar RAM — funciona en todos los dispositivos
        onProgress("Liberando memoria RAM...")
        appsKilled = killBackgroundProcesses(profile.killAggressiveness, onProgress)
        actions.add("$appsKilled procesos en background detenidos")
        delay(400)

        // 2. Trim de memoria — funciona en todos los dispositivos
        onProgress("Optimizando memoria del sistema...")
        sendMemoryTrim()
        Runtime.getRuntime().gc()
        actions.add("Memoria del sistema optimizada")
        delay(300)

        // 3. Limpiar caché propia
        try {
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
        } catch (e: Exception) {}

        // ── A partir de aquí requiere WRITE_SECURE_SETTINGS ──
        if (hasPrivileges) {

            // 4. Animaciones reducidas
            if (profile.reducedAnimations) {
                onProgress("Reduciendo animaciones...")
                val ok = applyAnimationSettings(0.5f)
                actions.add(if (ok) "Animaciones reducidas a 0.5x" else "Animaciones: no aplicadas (MIUI restringido)")
                delay(200)
            }

            // 5. WiFi/BLE scan pasivo
            if (profile.disableWifiScan) {
                onProgress("Optimizando escaneo WiFi...")
                PrivilegedHelper.putGlobalInt(resolver, "wifi_scan_always_enabled", 0)
                PrivilegedHelper.putGlobalInt(resolver, "ble_scan_always_enabled", 0)
                actions.add("Escaneo WiFi/BLE pasivo desactivado")
                delay(200)
            }

            // 6. Sync automático
            if (profile.pauseSync) {
                onProgress("Pausando sincronización...")
                PrivilegedHelper.putGlobalInt(resolver, "sync_disabled", 1)
                actions.add("Sincronización automática pausada")
            }

            // 7. Doze profundo + forzar entrada inmediata
            if (profile.enableDoze) {
                onProgress("Activando modo Doze...")
                PrivilegedHelper.exec("dumpsys deviceidle enable deep")
                PrivilegedHelper.exec("dumpsys deviceidle enable light")
                PrivilegedHelper.exec("dumpsys deviceidle force-idle deep")
                actions.add("Modo Doze profundo activado y forzado")
            }

            // 8. Restricción de datos background de apps pesadas
            if (profile.restrictBackgroundData) {
                onProgress("Restringiendo datos en segundo plano...")
                heavyApps.forEach { pkg ->
                    PrivilegedHelper.exec("cmd netpolicy set restrict-background true $pkg")
                }
                actions.add("Datos background de apps pesadas restringidos")
                delay(200)
            }

            // 9. Limpiar caché DNS del sistema
            onProgress("Limpiando caché DNS...")
            PrivilegedHelper.exec("ndc resolver clearnetdns")
            actions.add("Caché DNS limpiada")

            // 10. Cancelar trabajos diferidos pendientes que consumen recursos
            onProgress("Cancelando tareas diferidas...")
            PrivilegedHelper.exec("cmd jobscheduler reset-execution-quota")
            actions.add("Cuota de tareas diferidas reiniciada")

        } else {
            actions.add("⚠ Optimización avanzada no disponible — ejecuta el comando ADB en Ajustes")
        }

        onProgress("¡Optimización completada!")
        val ramAfter = getFreeRamMb()
        OptimizationResult(
            actionsTaken = actions,
            ramFreedMb = maxOf(0L, ramAfter - ramBefore),
            cacheFreedMb = 0L,
            appsKilled = appsKilled,
            temperatureBefore = temperatureBefore,
            success = true
        )
    }

    private fun sendMemoryTrim() {
        try {
            val processes = activityManager.runningAppProcesses ?: return
            processes.forEach { proc ->
                if (proc.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE &&
                    !systemProtected.any { proc.processName.startsWith(it) }
                ) {
                    activityManager.killBackgroundProcesses(proc.processName)
                }
            }
        } catch (e: Exception) {}
    }

    private suspend fun killBackgroundProcesses(
        level: KillLevel,
        onProgress: (String) -> Unit
    ): Int {
        var killed = 0
        val runningApps = activityManager.runningAppProcesses ?: return 0

        runningApps.forEach { proc ->
            if (proc.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED &&
                !systemProtected.any { proc.processName.startsWith(it) }
            ) {
                try {
                    activityManager.killBackgroundProcesses(proc.processName)
                    killed++
                } catch (e: Exception) {}
            }
        }

        // Force-stop de apps pesadas via shell (funciona si hay permisos suficientes)
        if (level != KillLevel.LIGHT) {
            val appsToKill = when (level) {
                KillLevel.MODERATE -> heavyApps
                KillLevel.AGGRESSIVE, KillLevel.MAXIMUM -> heavyApps + getInstalledUserApps()
                else -> emptySet()
            }
            appsToKill.forEach { pkg ->
                if (!systemProtected.any { pkg.startsWith(it) }) {
                    val r = PrivilegedHelper.exec("am force-stop $pkg")
                    if (r.success) killed++
                }
            }
            PrivilegedHelper.exec("am kill-all")
        }

        return killed
    }

    private suspend fun applyAnimationSettings(scale: Float): Boolean {
        val s = scale.toString()
        val r1 = PrivilegedHelper.putGlobalFloat(resolver, "window_animation_scale", scale)
        val r2 = PrivilegedHelper.putGlobalFloat(resolver, "transition_animation_scale", scale)
        val r3 = PrivilegedHelper.putGlobalFloat(resolver, "animator_duration_scale", scale)
        // Fallback via shell si las API fallan (MIUI)
        if (!r1 || !r2 || !r3) {
            PrivilegedHelper.exec("settings put global window_animation_scale $s")
            PrivilegedHelper.exec("settings put global transition_animation_scale $s")
            PrivilegedHelper.exec("settings put global animator_duration_scale $s")
        }
        return r1 && r2 && r3
    }

    private fun getInstalledUserApps(): Set<String> {
        return try {
            context.packageManager.getInstalledPackages(0)
                .filter { it.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }
                .map { it.packageName }
                .toSet()
        } catch (e: Exception) { emptySet() }
    }

    private fun getFreeRamMb(): Long {
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        return info.availMem / 1024 / 1024
    }

    suspend fun forceStopApp(packageName: String): Boolean {
        activityManager.killBackgroundProcesses(packageName)
        PrivilegedHelper.exec("am force-stop $packageName")
        return true
    }
}
