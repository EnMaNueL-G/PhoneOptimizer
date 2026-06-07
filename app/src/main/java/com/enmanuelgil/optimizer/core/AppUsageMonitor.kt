package com.enmanuelgil.optimizer.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo

data class AppMemoryInfo(
    val packageName: String,
    val appName: String,
    val memoryMb: Int,
    val isSystem: Boolean
)

class AppUsageMonitor(private val context: Context) {

    private val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val pm = context.packageManager

    fun getTopApps(limit: Int = 12): List<AppMemoryInfo> {
        val processes = am.runningAppProcesses ?: return emptyList()
        val pids = processes.map { it.pid }.toIntArray()
        val memInfos = try {
            am.getProcessMemoryInfo(pids)
        } catch (e: Exception) { return emptyList() }

        return processes.zip(memInfos.toList())
            .mapNotNull { (proc, mem) ->
                val pkg = proc.processName.split(":").first()
                if (pkg == context.packageName) return@mapNotNull null
                val mb = mem.totalPss / 1024
                if (mb <= 0) return@mapNotNull null
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) { pkg.substringAfterLast(".") }
                val isSystem = try {
                    pm.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM != 0
                } catch (e: Exception) { false }
                AppMemoryInfo(pkg, appName, mb, isSystem)
            }
            .sortedByDescending { it.memoryMb }
            .take(limit)
    }
}
