package com.enmanuelgil.optimizer.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import java.io.File

data class AppMemoryInfo(
    val packageName: String,
    val appName: String,
    val memoryMb: Int,
    val isSystem: Boolean
)

class AppUsageMonitor(private val context: Context) {

    private val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val pm = context.packageManager

    @Suppress("DEPRECATION")
    fun getTopApps(limit: Int = 12): List<AppMemoryInfo> {
        // getRunningServices() works on all Android versions including 12+
        // where getRunningAppProcesses() is restricted to the own process only.
        val services = try { am.getRunningServices(500) } catch (e: Exception) { null }

        val byPkg = mutableMapOf<String, MutableList<Int>>() // pkg -> pids

        // Collect PIDs from running services
        services?.forEach { svc ->
            val pkg = svc.service.packageName
            if (pkg != context.packageName && pkg.isNotBlank()) {
                byPkg.getOrPut(pkg) { mutableListOf() }.add(svc.pid)
            }
        }

        // Also try own-process list (limited but available)
        try {
            am.runningAppProcesses?.forEach { proc ->
                val pkg = proc.processName.split(":").first()
                if (pkg != context.packageName) {
                    byPkg.getOrPut(pkg) { mutableListOf() }.add(proc.pid)
                }
            }
        } catch (e: Exception) {}

        if (byPkg.isEmpty()) return emptyList()

        return byPkg.mapNotNull { (pkg, pids) ->
            // Verify it's an installed package
            val appName = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (e: Exception) { return@mapNotNull null }

            val isSystem = try {
                pm.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM != 0
            } catch (e: Exception) { false }

            // Try AM API first, fall back to /proc/pid/status (VmRSS)
            val uniquePids = pids.distinct()
            val memMb = getMemoryMb(uniquePids)
            if (memMb <= 0) return@mapNotNull null

            AppMemoryInfo(pkg, appName, memMb, isSystem)
        }
        .sortedByDescending { it.memoryMb }
        .take(limit)
    }

    private fun getMemoryMb(pids: List<Int>): Int {
        // Try ActivityManager API
        return try {
            val arr = pids.take(4).toIntArray() // API limit
            val infos = am.getProcessMemoryInfo(arr)
            val total = infos.sumOf { it.totalPss } / 1024
            if (total > 0) total
            else fallbackMemMb(pids)
        } catch (e: Exception) {
            fallbackMemMb(pids)
        }
    }

    // Read VmRSS from /proc/<pid>/status — works when AM API is rate-limited
    private fun fallbackMemMb(pids: List<Int>): Int {
        return pids.sumOf { pid ->
            try {
                File("/proc/$pid/status").readLines()
                    .firstOrNull { it.startsWith("VmRSS:") }
                    ?.replace(Regex("[^0-9]"), "")
                    ?.toIntOrNull()
                    ?.div(1024) ?: 0
            } catch (e: Exception) { 0 }
        }
    }
}
