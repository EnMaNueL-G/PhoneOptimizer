package com.enmanuelgil.optimizer.core

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

/** Uso real de una app en las últimas 24 h (dato de Android, no estimado). */
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val foregroundMinutes: Long,
    val lastUsed: Long,
    val isSystem: Boolean
)

/**
 * Desde Android 8 ninguna app normal puede ver qué procesos tienen abiertos las demás ni
 * cuánta RAM gastan. Lo que sí da Android (con el permiso "Acceso a datos de uso", que activa
 * el usuario) es cuánto tiempo se usó cada app: eso es lo que se muestra aquí.
 */
class AppUsageMonitor(private val context: Context) {

    private val pm = context.packageManager

    fun hasUsageAccess(): Boolean = try {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        else
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) { false }

    /** Abre el ajuste de Android donde se concede "Acceso a datos de uso". */
    fun openUsageAccessSettings() {
        val direct = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS,
            Uri.parse("package:${context.packageName}"))
        for (i in listOf(direct, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))) {
            try { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (_: Exception) {}
        }
    }

    /** Apps más usadas en las últimas 24 h, de más a menos tiempo en pantalla. */
    fun getTopApps(limit: Int = 20): List<AppUsageInfo> {
        if (!hasUsageAccess()) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 24L * 3600_000L
        val stats = try { usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end) } catch (_: Exception) { null }
            ?: return emptyList()

        // Una app puede venir en varios intervalos: se suman
        val byPkg = HashMap<String, Pair<Long, Long>>()  // pkg -> (ms en primer plano, última vez)
        for (u in stats) {
            if (u.lastTimeUsed < start) continue
            val prev = byPkg[u.packageName] ?: (0L to 0L)
            byPkg[u.packageName] = (prev.first + u.totalTimeInForeground) to maxOf(prev.second, u.lastTimeUsed)
        }

        return byPkg.mapNotNull { (pkg, v) ->
            if (pkg == context.packageName || v.first < 60_000L) return@mapNotNull null
            val info = try { pm.getApplicationInfo(pkg, 0) } catch (_: Exception) { return@mapNotNull null }
            // Solo apps con icono en el menú (fuera launcher, teclado y servicios internos)
            if (pm.getLaunchIntentForPackage(pkg) == null) return@mapNotNull null
            AppUsageInfo(
                packageName = pkg,
                appName = pm.getApplicationLabel(info).toString(),
                foregroundMinutes = v.first / 60_000L,
                lastUsed = v.second,
                isSystem = info.flags and ApplicationInfo.FLAG_SYSTEM != 0
            )
        }
            .sortedByDescending { it.foregroundMinutes }
            .take(limit)
    }
}
