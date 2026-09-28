package com.enmanuelgil.optimizer.core

import android.content.Context
import android.provider.Settings

/**
 * Bloqueo de anuncios con el "DNS privado" de Android apuntando a AdGuard DNS.
 * Guarda el DNS privado que hubiera antes para devolverlo al desactivar.
 * Limitaciones: no quita los anuncios de YouTube (vienen del mismo servidor que los vídeos)
 * y en redes que bloquean el DNS privado (algunos hoteles o empresas) puede cortar internet.
 */
object AdBlockManager {
    private const val DNS_MODE_KEY = "private_dns_mode"
    private const val DNS_HOST_KEY = "private_dns_specifier"
    const val ADBLOCK_DNS = "dns.adguard-dns.com"
    private val KNOWN_HOSTS = setOf(ADBLOCK_DNS, "dns.adguard.com")

    private const val PREFS = "adblock_backup"
    private const val K_MODE = "mode"
    private const val K_HOST = "host"

    fun isEnabled(context: Context): Boolean {
        val r = context.contentResolver
        return try {
            Settings.Global.getString(r, DNS_MODE_KEY) == "hostname" &&
                Settings.Global.getString(r, DNS_HOST_KEY) in KNOWN_HOSTS
        } catch (_: Exception) { false }
    }

    fun enable(context: Context): Boolean {
        val r = context.contentResolver
        return try {
            if (!isEnabled(context)) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(K_MODE, Settings.Global.getString(r, DNS_MODE_KEY))
                    .putString(K_HOST, Settings.Global.getString(r, DNS_HOST_KEY))
                    .apply()
            }
            Settings.Global.putString(r, DNS_MODE_KEY, "hostname")
            Settings.Global.putString(r, DNS_HOST_KEY, ADBLOCK_DNS)
            isEnabled(context)
        } catch (_: Exception) { false }
    }

    fun disable(context: Context): Boolean {
        val r = context.contentResolver
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return try {
            if (p.contains(K_MODE) || p.contains(K_HOST)) {
                // Devolver exactamente lo que había (aunque fuera "sin valor" = por defecto)
                val mode = p.getString(K_MODE, null)
                val host = p.getString(K_HOST, null)
                Settings.Global.putString(r, DNS_HOST_KEY, if (host in KNOWN_HOSTS) null else host)
                Settings.Global.putString(r, DNS_MODE_KEY,
                    if (mode == "hostname" && (host.isNullOrBlank() || host in KNOWN_HOSTS)) "opportunistic" else mode)
            } else {
                // Sin copia (activado con una versión antigua): modo automático
                Settings.Global.putString(r, DNS_MODE_KEY, "opportunistic")
            }
            p.edit().clear().apply()
            !isEnabled(context)
        } catch (_: Exception) { false }
    }
}
