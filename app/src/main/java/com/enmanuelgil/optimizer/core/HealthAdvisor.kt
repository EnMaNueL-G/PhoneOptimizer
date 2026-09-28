package com.enmanuelgil.optimizer.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.enmanuelgil.optimizer.model.DeviceStats
import com.enmanuelgil.optimizer.model.ThermalStatus

/**
 * Recomendaciones basadas en lo que de verdad hace lento o calienta un teléfono.
 * Cada consejo lleva, si existe, un botón que abre el ajuste de Android donde se soluciona.
 */
object HealthAdvisor {

    enum class Action(val label: String) {
        STORAGE("Liberar espacio"),
        RESTART_HELP("Cómo reiniciar"),
        BATTERY_SAVER("Ahorro de batería"),
        BATTERY_INFO("Ver batería"),
        NONE("")
    }

    enum class Level { INFO, WARN, ALERT }

    data class Tip(
        val id: String,
        val title: String,
        val detail: String,
        val level: Level,
        val action: Action = Action.NONE
    ) {
        /** Merece notificación en la revisión automática. */
        val important: Boolean get() = level != Level.INFO
    }

    fun check(s: DeviceStats): List<Tip> {
        val tips = mutableListOf<Tip>()

        // 1. Almacenamiento: la causa nº 1 de lentitud
        val st = s.storageUsagePercent
        if (s.storageTotalGb > 0 && st >= 90f) {
            tips += Tip("storage_full", "Almacenamiento casi lleno (${st.toInt()} %)",
                "Con menos del 10 % libre Android se vuelve lento y las apps fallan. " +
                "Borra vídeos que ya tengas guardados, descargas viejas y apps que no uses.",
                Level.ALERT, Action.STORAGE)
        } else if (s.storageTotalGb > 0 && st >= 80f) {
            tips += Tip("storage_high", "Almacenamiento al ${st.toInt()} %",
                "Conviene mantener al menos un 15-20 % libre para que el teléfono vaya fluido.",
                Level.WARN, Action.STORAGE)
        }

        // 2. Días sin reiniciar
        val days = s.uptimeHours / 24
        if (days >= 7) {
            tips += Tip("uptime", "$days días sin reiniciar",
                "Reiniciar una vez por semana limpia la memoria y los procesos atascados de verdad. " +
                "Es lo que más ayuda a un teléfono lento.",
                Level.WARN, Action.RESTART_HELP)
        }

        // 3. Calor
        val hot = s.temperatureBattery >= 42f || s.thermalStatus.ordinal >= ThermalStatus.SEVERE.ordinal
        if (hot && s.batteryCharging) {
            tips += Tip("hot_charging", "Caliente mientras carga (%.0f °C)".format(s.temperatureBattery),
                "Usar el teléfono mientras carga es lo que más lo calienta y desgasta la batería. " +
                "Desconéctalo un rato, quita la funda y no lo cargues al sol o sobre la cama.",
                Level.ALERT)
        } else if (hot) {
            tips += Tip("hot", "Teléfono caliente (%.0f °C)".format(s.temperatureBattery),
                "Cierra juegos, cámara o GPS si no los usas, baja el brillo y quita la funda. " +
                "El ahorro de batería de Android limita la potencia y ayuda a enfriar.",
                Level.ALERT, Action.BATTERY_SAVER)
        } else if (s.thermalStatus == ThermalStatus.MODERATE || s.temperatureBattery >= 39f) {
            tips += Tip("warm", "Algo tibio (%.0f °C)".format(s.temperatureBattery),
                "Normal si estás jugando o grabando vídeo. Si está así sin usarlo, revisa qué apps gastan más batería.",
                Level.INFO, Action.BATTERY_INFO)
        }

        // 4. Salud de la batería
        if (s.batteryHealth.bad) {
            tips += Tip("battery_health", "Batería: ${s.batteryHealth.label.lowercase()}",
                "Android indica un problema en la batería. Si se descarga muy rápido o se hincha, " +
                "llévala a revisar a un servicio técnico.",
                Level.ALERT, Action.BATTERY_INFO)
        }

        // 5. Batería baja
        if (!s.batteryCharging && s.batteryLevel in 1..15) {
            tips += Tip("battery_low", "Batería baja (${s.batteryLevel} %)",
                "Activa el ahorro de batería para llegar más lejos.",
                Level.INFO, Action.BATTERY_SAVER)
        }

        return tips
    }

    /** Abre la pantalla de Android correspondiente. Devuelve false si no existe en el teléfono. */
    fun open(context: Context, action: Action): Boolean {
        val candidates = when (action) {
            Action.STORAGE -> listOf(
                Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
                Intent(Settings.ACTION_SETTINGS))
            Action.BATTERY_SAVER -> listOf(
                Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
                Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
            Action.BATTERY_INFO -> listOf(
                Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
                Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
            Action.RESTART_HELP, Action.NONE -> emptyList()
        }
        for (i in candidates) {
            try {
                context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: Exception) {}
        }
        return false
    }

    /** Ficha de una app en Ajustes (para borrar caché, restringir batería, desinstalar). */
    fun openAppDetails(context: Context, pkg: String) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {}
    }
}
