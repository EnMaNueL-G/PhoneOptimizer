package com.enmanuelgil.optimizer.core

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reemplaza Shizuku con APIs nativas de Android.
 * WRITE_SECURE_SETTINGS se otorga una sola vez via ADB:
 *   adb shell pm grant <packageId> android.permission.WRITE_SECURE_SETTINGS
 */
object PrivilegedHelper {

    fun hasWriteSecureSettings(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun putGlobal(resolver: ContentResolver, key: String, value: String): Boolean {
        return try {
            Settings.Global.putString(resolver, key, value)
            true
        } catch (e: SecurityException) { false }
    }

    fun putGlobalFloat(resolver: ContentResolver, key: String, value: Float): Boolean {
        return try {
            Settings.Global.putFloat(resolver, key, value)
            true
        } catch (e: SecurityException) { false }
    }

    fun putGlobalInt(resolver: ContentResolver, key: String, value: Int): Boolean {
        return try {
            Settings.Global.putInt(resolver, key, value)
            true
        } catch (e: SecurityException) { false }
    }

    suspend fun exec(command: String): ExecResult = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            ExecResult(exitCode == 0, output.trim(), if (error.isNotBlank()) error.trim() else null)
        } catch (e: Exception) {
            ExecResult(false, "", e.message)
        }
    }

    data class ExecResult(
        val success: Boolean,
        val output: String = "",
        val error: String? = null
    )
}
