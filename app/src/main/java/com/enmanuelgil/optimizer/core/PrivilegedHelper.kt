package com.enmanuelgil.optimizer.core

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Ajustes del sistema que se pueden cambiar con WRITE_SECURE_SETTINGS.
 * Ese permiso se concede una sola vez desde un PC:
 *   adb shell pm grant <paquete> android.permission.WRITE_SECURE_SETTINGS
 *
 * Antes de cambiar un ajuste por primera vez se guarda su valor original, para poder
 * dejarlo todo como estaba con [restoreAll].
 */
object PrivilegedHelper {

    /** Ajustes que la app puede tocar (y por tanto restaurar). */
    val MANAGED_KEYS = listOf(
        "window_animation_scale",
        "transition_animation_scale",
        "animator_duration_scale",
        "wifi_scan_always_enabled",
        "ble_scan_always_enabled"
    )

    private const val PREFS = "settings_backup"
    /** Marca de "el ajuste no existía" (se restaura borrándolo = valor por defecto). */
    private const val ABSENT = "\u0000absent"

    fun hasWriteSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun getGlobal(resolver: ContentResolver, key: String): String? =
        try { Settings.Global.getString(resolver, key) } catch (_: Exception) { null }

    /**
     * Escribe un ajuste y lo relee para confirmar que quedó puesto.
     * Devuelve true solo si el valor leído después coincide.
     */
    fun putGlobalVerified(context: Context, key: String, value: String): Boolean {
        val resolver = context.contentResolver
        backupOnce(context, key)
        return try {
            Settings.Global.putString(resolver, key, value)
            sameValue(getGlobal(resolver, key), value)
        } catch (_: Exception) { false }
    }

    private fun sameValue(read: String?, expected: String): Boolean {
        if (read == expected) return true
        val a = read?.toFloatOrNull() ?: return false
        val b = expected.toFloatOrNull() ?: return false
        return kotlin.math.abs(a - b) < 0.001f
    }

    private fun backupOnce(context: Context, key: String) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.contains(key)) return
        p.edit().putString(key, getGlobal(context.contentResolver, key) ?: ABSENT).apply()
    }

    /** ¿Hay ajustes cambiados por la app que se puedan restaurar? */
    fun hasBackup(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all.isNotEmpty()

    /** Estado actual de las animaciones (1.0 si el ajuste no existe). */
    fun animationScale(context: Context): Float =
        getGlobal(context.contentResolver, "window_animation_scale")?.toFloatOrNull() ?: 1f

    /**
     * Devuelve cada ajuste a su valor original. Devuelve cuántos se restauraron y cuántos fallaron.
     */
    fun restoreAll(context: Context): Pair<Int, Int> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val resolver = context.contentResolver
        var ok = 0
        var fail = 0
        val done = mutableListOf<String>()
        for ((key, v) in p.all) {
            val original = v as? String ?: continue
            try {
                if (original == ABSENT) {
                    // No existía: los valores por defecto de Android son 1.0 (animaciones) y
                    // "según el fabricante" (escaneo). Para el escaneo se deja en 1 (activado),
                    // que es el valor por defecto en casi todos los teléfonos.
                    val def = if (key.endsWith("_scale")) "1.0" else "1"
                    Settings.Global.putString(resolver, key, def)
                } else {
                    Settings.Global.putString(resolver, key, original)
                }
                ok++; done += key
            } catch (_: Exception) { fail++ }
        }
        if (done.isNotEmpty()) p.edit().apply { done.forEach { remove(it) } }.apply()
        return ok to fail
    }
}
