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
import com.enmanuelgil.optimizer.R
import com.enmanuelgil.optimizer.core.HealthAdvisor
import com.enmanuelgil.optimizer.core.SystemMonitor
import java.util.concurrent.TimeUnit

/**
 * Revisión automática periódica del teléfono.
 *
 * Solo MIRA y avisa si algo necesita atención (almacenamiento casi lleno, muchos días sin
 * reiniciar, batería dañada...). No cierra apps ni cambia ajustes por su cuenta: antes lo hacía
 * cada 6 h sin preguntar y podía cortar una subida de fotos o dejar ajustes cambiados.
 */

// ── Preferencias ────────────────────────────────────────────────────────────
object MaintenancePrefs {
    private const val FILE = "auto_maintenance"
    // Clave nueva: la antigua venía ACTIVADA por defecto; así nadie queda con ella encendida
    // sin haberlo elegido.
    private const val K_ENABLED = "checkup_enabled"
    private const val K_INTERVAL = "checkup_interval_hours"
    private const val K_LAST_ISSUES = "last_issues"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(K_ENABLED, false)
    fun intervalHours(ctx: Context): Int = prefs(ctx).getInt(K_INTERVAL, 24)

    fun setEnabled(ctx: Context, enabled: Boolean) =
        prefs(ctx).edit().putBoolean(K_ENABLED, enabled).apply()
    fun setIntervalHours(ctx: Context, hours: Int) =
        prefs(ctx).edit().putInt(K_INTERVAL, hours).apply()

    /** Para no repetir el mismo aviso en cada revisión. */
    fun lastIssues(ctx: Context): String = prefs(ctx).getString(K_LAST_ISSUES, "") ?: ""
    fun setLastIssues(ctx: Context, v: String) = prefs(ctx).edit().putString(K_LAST_ISSUES, v).apply()
}

// ── Programador ─────────────────────────────────────────────────────────────
object MaintenanceScheduler {
    private const val WORK_NAME = "auto_maintenance_periodic"

    fun enable(context: Context, hours: Int) {
        val safeHours = hours.coerceIn(6, 72).toLong()
        val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(safeHours, TimeUnit.HOURS)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Reaplica el estado guardado (al abrir la app y tras reiniciar el teléfono). */
    fun applyFromPrefs(context: Context) {
        if (MaintenancePrefs.isEnabled(context)) enable(context, MaintenancePrefs.intervalHours(context))
        else disable(context)
    }
}

// ── Worker ──────────────────────────────────────────────────────────────────
class MaintenanceWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val stats = SystemMonitor(appContext).getStats()
            val issues = HealthAdvisor.check(stats).filter { it.important }
            val key = issues.joinToString("|") { it.id }
            if (issues.isNotEmpty() && key != MaintenancePrefs.lastIssues(appContext)) {
                notify(issues.first().title, issues.joinToString(" · ") { it.title })
            }
            MaintenancePrefs.setLastIssues(appContext, key)
            Result.success()
        } catch (_: Exception) {
            Result.success()   // no reintentar en bucle: la próxima revisión llegará sola
        }
    }

    private fun notify(title: String, text: String) {
        try {
            val nm = appContext.getSystemService(NotificationManager::class.java) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Revisión automática", NotificationManager.IMPORTANCE_DEFAULT)
                        .apply { description = "Aviso cuando la revisión encuentra algo que mejorar" }
                )
            }
            val pi = PendingIntent.getActivity(
                appContext, 0, Intent(appContext, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notif = NotificationCompat.Builder(appContext, CHANNEL)
                .setSmallIcon(R.drawable.ic_bolt)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$text\nToca para ver cómo solucionarlo."))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            nm.notify(NOTIF_ID, notif)
        } catch (_: Exception) {}
    }

    companion object {
        private const val CHANNEL = "auto_checkup"
        private const val NOTIF_ID = 1003
    }
}
