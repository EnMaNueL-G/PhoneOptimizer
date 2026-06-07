package com.enmanuelgil.optimizer.core

import android.content.ContentResolver
import android.provider.Settings

object AdBlockManager {
    private const val DNS_MODE_KEY = "private_dns_mode"
    private const val DNS_HOST_KEY = "private_dns_specifier"
    private const val ADBLOCK_DNS = "dns.adguard.com"

    fun isEnabled(resolver: ContentResolver): Boolean =
        Settings.Global.getString(resolver, DNS_MODE_KEY) == "hostname" &&
        Settings.Global.getString(resolver, DNS_HOST_KEY) == ADBLOCK_DNS

    fun enable(resolver: ContentResolver): Boolean {
        return try {
            Settings.Global.putString(resolver, DNS_MODE_KEY, "hostname")
            Settings.Global.putString(resolver, DNS_HOST_KEY, ADBLOCK_DNS)
            true
        } catch (e: SecurityException) { false }
    }

    fun disable(resolver: ContentResolver): Boolean {
        return try {
            Settings.Global.putString(resolver, DNS_MODE_KEY, "opportunistic")
            Settings.Global.putString(resolver, DNS_HOST_KEY, "")
            true
        } catch (e: SecurityException) { false }
    }
}
