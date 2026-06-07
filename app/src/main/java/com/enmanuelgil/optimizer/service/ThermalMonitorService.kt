package com.enmanuelgil.optimizer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Build
import androidx.core.app.NotificationCompat
import com.enmanuelgil.optimizer.MainActivity
import com.enmanuelgil.optimizer.R
import com.enmanuelgil.optimizer.core.OptimizationEngine
import com.enmanuelgil.optimizer.core.SystemMonitor
import com.enmanuelgil.optimizer.model.OptimizationProfile
import com.enmanuelgil.optimizer.model.ThermalStatus
import kotlinx.coroutines.*

class ThermalMonitorService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var monitor: SystemMonitor
    private lateinit var engine: OptimizationEngine
    private var lastAlertTemp = 0f

    companion object {
        const val CHANNEL_ID = "thermal_monitor"
        const val NOTIF_ID = 1001
        const val ALERT_NOTIF_ID = 1002
        const val ACTION_OPTIMIZE = "com.enmanuelgil.optimizer.ACTION_OPTIMIZE"

        fun start(context: Context) {
            try {
                val intent = Intent(context, ThermalMonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Ignorar si falla el inicio del servicio
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, ThermalMonitorService::class.java))
            } catch (e: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        monitor = SystemMonitor(this)
        engine = OptimizationEngine(this)
        createNotificationChannel()
        try {
            startForeground(NOTIF_ID, buildBaseNotification())
        } catch (e: Exception) {
            // En Android 14+ puede fallar si no hay permiso exacto — continuar sin foreground
        }
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_OPTIMIZE) {
            scope.launch {
                try {
                    engine.optimize(OptimizationProfile.THERMAL_GUARD)
                } catch (e: Exception) {}
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        scope.launch {
            while (isActive) {
                try {
                    val stats = monitor.getStats()
                    safeUpdateNotification(stats.temperatureCpu, stats.thermalStatus, stats.batteryLevel)

                    if (stats.temperatureCpu > 45f && stats.temperatureCpu > lastAlertTemp + 2f) {
                        lastAlertTemp = stats.temperatureCpu
                        safeSendAlert(stats.temperatureCpu, stats.thermalStatus)
                    }

                    if (stats.thermalStatus == ThermalStatus.SEVERE ||
                        stats.thermalStatus == ThermalStatus.CRITICAL) {
                        engine.optimize(OptimizationProfile.THERMAL_GUARD)
                    }
                } catch (e: Exception) {
                    // Ignorar errores individuales de monitoreo
                }
                delay(10_000)
            }
        }
    }

    private fun safeUpdateNotification(temp: Float, status: ThermalStatus, battery: Int) {
        try {
            val tempStr = if (temp > 0) "%.1f°C".format(temp) else "—"
            val notif = buildBaseNotificationBuilder()
                .setContentText("Temperatura: $tempStr | Batería: $battery% | ${status.label}")
                .build()
            getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, notif)
        } catch (e: Exception) {}
    }

    private fun safeSendAlert(temp: Float, status: ThermalStatus) {
        try {
            val intent = Intent(this, ThermalMonitorService::class.java).apply {
                action = ACTION_OPTIMIZE
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT

            val pi = PendingIntent.getService(this, 0, intent, flags)
            val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠ Temperatura Elevada — ${status.label}")
                .setContentText("CPU a %.1f°C. Toca para optimizar ahora.".format(temp))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(android.R.drawable.ic_media_play, "Optimizar", pi)
                .setAutoCancel(true)
                .build()

            getSystemService(NotificationManager::class.java)?.notify(ALERT_NOTIF_ID, notif)
        } catch (e: Exception) {}
    }

    private fun buildBaseNotification() = buildBaseNotificationBuilder().build()

    private fun buildBaseNotificationBuilder(): NotificationCompat.Builder {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), flags
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("PhoneOptimizer — Activo")
            .setContentText("Monitoreando temperatura y rendimiento")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .setOngoing(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitor Térmico",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoreo de temperatura y rendimiento"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
