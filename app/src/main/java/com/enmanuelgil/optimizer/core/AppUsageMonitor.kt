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

    fun getTopApps(limit: Int = 12): List<AppMemoryInfo> {
        val fromApi = getFromActivityManager()
        // Android 12+ restricts getRunningAppProcesses to own processes only —
        // fall back to /proc if we get fewer than 3 results
        val results = if (fromApi.size >= 3) fromApi else mergeWithProc(fromApi)
        return results.sortedByDescending { it.memoryMb }.take(limit)
    }

    private fun getFromActivityManager(): List<AppMemoryInfo> {
        val processes = am.runningAppProcesses ?: return emptyList()
        val pids = processes.map { it.pid }.toIntArray()
        val memInfos = try { am.getProcessMemoryInfo(pids) } catch (e: Exception) { return emptyList() }
        return processes.zip(memInfos.toList()).mapNotNull { (proc, mem) ->
            val pkg = proc.processName.split(":").first()
            if (pkg == context.packageName) return@mapNotNull null
            val mb = mem.totalPss / 1024
            if (mb <= 0) return@mapNotNull null
            AppMemoryInfo(pkg, resolveAppName(pkg), mb, isSystemApp(pkg))
        }
    }

    private fun mergeWithProc(existing: List<AppMemoryInfo>): List<AppMemoryInfo> {
        val existingPkgs = existing.map { it.packageName }.toSet()
        val procApps = readFromProc().filter { it.packageName !in existingPkgs }
        return existing + procApps
    }

    // Reads process memory from /proc filesystem — works on all Android versions
    // without ActivityManager restrictions.
    private fun readFromProc(): List<AppMemoryInfo> {
        val procDir = File("/proc")
        val pidDirs = procDir.listFiles { f ->
            f.isDirectory && f.name.all { it.isDigit() }
        } ?: return emptyList()

        return pidDirs.mapNotNull { pidDir ->
            try {
                val cmdline = File(pidDir, "cmdline").readBytes()
                    .takeWhile { it != 0.toByte() }
                    .toByteArray()
                    .toString(Charsets.UTF_8)
                    .trim()

                if (cmdline.isBlank() || !cmdline.contains(".")) return@mapNotNull null

                val pkg = cmdline.split(":").first()
                if (pkg == context.packageName) return@mapNotNull null
                // Only consider known installed packages to avoid kernel/system noise
                try { pm.getApplicationInfo(pkg, 0) } catch (e: Exception) { return@mapNotNull null }

                val rssKb = File(pidDir, "status").readLines()
                    .firstOrNull { it.startsWith("VmRSS:") }
                    ?.replace(Regex("[^0-9]"), "")
                    ?.toLongOrNull() ?: 0L

                val mb = (rssKb / 1024).toInt()
                if (mb < 5) return@mapNotNull null

                AppMemoryInfo(pkg, resolveAppName(pkg), mb, isSystemApp(pkg))
            } catch (e: Exception) { null }
        }
    }

    private fun resolveAppName(pkg: String): String = try {
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) { pkg.substringAfterLast(".") }

    private fun isSystemApp(pkg: String): Boolean = try {
        pm.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM != 0
    } catch (e: Exception) { false }
}
