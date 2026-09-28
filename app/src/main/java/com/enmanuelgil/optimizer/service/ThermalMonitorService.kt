package com.enmanuelgil.optimizer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.enmanuelgil.optimizer.MainActivity
import com.enmanuelgil.optimizer.R
import com.enmanuelgil.optimizer.core.SystemMonitor
import com.enmanuelgil.optimizer.model.ThermalStatus
import kotlinx.coroutines.*

/** Preferencia del monitor térmico (apagado por defecto: el usuario lo activa). */
object MonitorPrefs {
    private const val FILE = "thermal_monitor"
    private const val K_ENABLED = "enabled"
    fun isEnabled(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(K_ENABLED, false)
    fun setEnabled(ctx: Context, on: Boolean) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(K_ENABLED, on).apply()
}

/**
 * Vigila la temperatura y AVISA con consejos cuando el teléfono se calienta.
 *
 * No hace nada automático (antes cerraba apps cada 10 s con el teléfono caliente, lo que
 * añadía más carga justo cuando menos convenía). Mira cada minuto y solo con la pantalla
 * encendida, que es cuando el teléfono se calienta por uso.
 */
class ThermalMonitorService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var monitor: SystemMonitor
    private var alerted = false
    private var lastText = ""

    companion object {
        const val CHANNEL_ID = "thermal_monitor"
        const val ALERT_CHANNEL_ID = "thermal_alerts"
        const val NOTIF_ID = 1001
        const val ALERT_NOTIF_ID = 1002
        private const val INTERVAL_MS = 60_000L
        /** Batería a partir de la cual se avisa (°C). */
        const val BATTERY_ALERT_C = 42f

        /** Arranca solo si el usuario lo activó. */
        fun startIfEnabled(context: Context) {
            if (MonitorPrefs.isEnabled(context)) start(context)
        }

        fun start(context: Context) {
            try {
                val intent = Intent(context, ThermalMonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            try { context.stopService(Intent(context, ThermalMonitorService::class.java)) } catch (_: Exception) {}
            try { context.getSystemService(NotificationManager::class.java)?.cancel(ALERT_NOTIF_ID) } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        monitor = SystemMonitor(this)
        createNotificationChannels()
        try {
            startForeground(NOTIF_ID, buildBaseNotification("Vigilando la temperatura"))
        } catch (_: Exception) {
            // Si Android no deja iniciarlo en primer plano hay que parar: si no, cierra la app.
            stopSelf()
            return
        }
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!MonitorPrefs.isEnabled(this)) { stopSelf(); return START_NOT_STICKY }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        scope.launch {
            while (isActive) {
                if (pm.isInteractive) {
                    try {
                        val s = monitor.getStats()
                        val hot = s.temperatureBattery >= BATTERY_ALERT_C ||
                            s.thermalStatus.ordinal >= ThermalStatus.SEVERE.ordinal
                        val tempStr = if (s.temperatureBattery > 0) "%.0f °C".format(s.temperatureBattery) else "—"
                        updateNotification("Batería $tempStr · ${s.thermalStatus.label}")

                        if (hot && !alerted) {
                            alerted = true
                            sendAlert(s.temperatureBattery, s.batteryCharging)
                        } else if (!hot && s.temperatureBattery < BATTERY_ALERT_C - 3f &&
                            s.thermalStatus.ordinal < ThermalStatus.MODERATE.ordinal) {
                            // Se enfrió: se podrá volver a avisar si se calienta otra vez
                            alerted = false
                        }
                    } catch (_: Exception) {}
                }
                delay(INTERVAL_MS)
            }
        }
    }

    private fun pendingOpenApp(): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), flags)
    }

    private fun updateNotification(text: String) {
        if (text == lastText) return   // no redibujar la notificación si no cambió
        lastText = text
        try {
            getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, buildBaseNotification(text))
        } catch (_: Exception) {}
    }

    private fun sendAlert(temp: Float, charging: Boolean) {
        try {
            val tip = if (charging)
                "Desconecta el cargador un rato y quita la funda."
            else
                "Cierra el juego o la cámara, baja el brillo y quita la funda."
            val notif = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("Teléfono caliente: batería a %.0f °C".format(temp))
                .setContentText(tip)
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "$tip Evita el sol directo. Abre la app para ver más consejos."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingOpenApp())
                .setAutoCancel(true)
                .build()
            getSystemService(NotificationManager::class.java)?.notify(ALERT_NOTIF_ID, notif)
        } catch (_: Exception) {}
    }

    private fun buildBaseNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_monitor)
            .setContentTitle("Monitor de temperatura")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pendingOpenApp())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Monitor de temperatura (fijo)", NotificationManager.IMPORTANCE_MIN)
                .apply { description = "Aviso permanente mientras el monitor está activo" }
        )
        nm.createNotificationChannel(
            NotificationChannel(ALERT_CHANNEL_ID, "Alertas de temperatura", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Aviso cuando el teléfono se calienta, con consejos" }
        )
    }
}
