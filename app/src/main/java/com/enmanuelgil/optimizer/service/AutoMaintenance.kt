package com.enmanuelgil.optimizer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.enmanuelgil.optimizer.MainActivity
import com.enmanuelgil.optimizer.core.HistoryManager
import com.enmanuelgil.optimizer.core.OptimizationEngine
import com.enmanuelgil.optimizer.model.OptimizationProfile
import com.enmanuelgil.optimizer.model.OptimizationRecord
import java.util.concurrent.TimeUnit

/**
 * Mantenimiento automático PERIÓDICO del teléfono, sin intervención manual.
 *
 * Usa WorkManager (sobrevive cierres de app y reinicios) para ejecutar la optimización
 * recomendada cada cierto intervalo. Complementa al ThermalMonitorService (que reacciona
 * en tiempo real al sobrecalentamiento): aquí garantizamos un mantenimiento de fondo
 * constante que mantiene el rendimiento sin que el usuario haga nada.
 */

// ── Preferencias (persistencia simple) ──────────────────────────────────────
object MaintenancePrefs {
    private const val FILE = "auto_maintenance"
    private const val K_ENABLED = "enabled"
    private const val K_INTERVAL = "interval_hours"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(K_ENABLED, true)  // por defecto ACTIVO
    fun intervalHours(ctx: Context): Int = prefs(ctx).getInt(K_INTERVAL, 6)

    fun setEnabled(ctx: Context, enabled: Boolean) =
        prefs(ctx).edit().putBoolean(K_ENABLED, enabled).apply()
    fun setIntervalHours(ctx: Context, hours: Int) =
        prefs(ctx).edit().putInt(K_INTERVAL, hours).apply()
}

// ── Programador ─────────────────────────────────────────────────────────────
object MaintenanceScheduler {
    private const val WORK_NAME = "auto_maintenance_periodic"

    /** Activa (o reprograma) el mantenimiento periódico cada [hours] horas. */
    fun enable(context: Context, hours: Int) {
        val safeHours = hours.coerceIn(1, 24).toLong()
        val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(safeHours, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Reaplica el estado guardado (llamar al iniciar la app y tras el arranque del sistema). */
    fun applyFromPrefs(context: Context) {
        if (MaintenancePrefs.isEnabled(context)) {
            enable(context, MaintenancePrefs.intervalHours(context))
        } else {
            disable(context)
        }
    }
}

// ── Worker ──────────────────────────────────────────────────────────────────
class MaintenanceWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val engine = OptimizationEngine(appContext)
            val result = engine.optimize(OptimizationProfile.RECOMMENDED)
            // Registrar en el historial para que el usuario vea el mantenimiento realizado.
            HistoryManager.save(
                appContext,
                OptimizationRecord(
                    profileName = "Mantenimiento automático",
                    ramFreedMb = result.ramFreedMb,
                    appsKilled = result.appsKilled,
                    temperatureBefore = 0f,
                    actionCount = result.actionsTaken.size
                )
            )
            notifyDone(result.ramFreedMb, result.appsKilled)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun notifyDone(ramFreedMb: Long, appsKilled: Int) {
        try {
            val nm = appContext.getSystemService(NotificationManager::class.java) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Mantenimiento automático", NotificationManager.IMPORTANCE_LOW)
                        .apply { description = "Resumen del mantenimiento periódico" }
                )
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
            val pi = PendingIntent.getActivity(appContext, 0, Intent(appContext, MainActivity::class.java), flags)
            val notif = NotificationCompat.Builder(appContext, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentTitle("Mantenimiento automático completado")
                .setContentText("$appsKilled procesos detenidos · ${ramFreedMb} MB de RAM liberados")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            nm.notify(NOTIF_ID, notif)
        } catch (_: Exception) {}
    }

    companion object {
        private const val CHANNEL = "auto_maintenance"
        private const val NOTIF_ID = 1003
    }
}
