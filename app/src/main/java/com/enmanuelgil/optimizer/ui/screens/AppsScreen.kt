package com.enmanuelgil.optimizer.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.optimizer.core.AppUsageInfo
import com.enmanuelgil.optimizer.core.HealthAdvisor
import com.enmanuelgil.optimizer.model.OptimizationRecord
import com.enmanuelgil.optimizer.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppsScreen(
    topApps: List<AppUsageInfo>,
    hasUsageAccess: Boolean,
    isLoading: Boolean,
    history: List<OptimizationRecord>,
    onRefreshApps: () -> Unit,
    onGrantUsage: () -> Unit,
    onClearHistory: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Cargar la lista una vez (y si llega el permiso); no en cada visita
    LaunchedEffect(hasUsageAccess) { if (hasUsageAccess && topApps.isEmpty()) onRefreshApps() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Más usadas", "Historial").forEachIndexed { index, label ->
                FilterChip(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    label = { Text(label, fontSize = 14.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f),
                        selectedLabelColor = PrimaryBlue
                    )
                )
            }
        }
        when (selectedTab) {
            0 -> if (hasUsageAccess) TopAppsTab(topApps, isLoading, onRefreshApps)
                 else UsageAccessCard(onGrantUsage)
            1 -> HistoryTab(history, onClearHistory)
        }
    }
}

@Composable
private fun UsageAccessCard(onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.QueryStats, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
            Text("Ver qué apps usas más", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 16.sp)
            Text(
                "Android solo deja ver cuánto tiempo se usa cada app si le das a esta app el permiso " +
                "\"Acceso a datos de uso\". Se queda en tu teléfono: la app no tiene acceso a internet para enviarlo.",
                fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp
            )
            Text(
                "En la pantalla que se abre, busca PhoneOptimizer y actívalo.",
                fontSize = 13.sp, color = TextSecondary
            )
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Dar permiso")
            }
        }
    }
}

@Composable
fun TopAppsTab(
    apps: List<AppUsageInfo>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tiempo en pantalla (24 h)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar lista", tint = PrimaryBlue)
            }
        }
        Text(
            "Toca una app para abrir su ficha de Android: ahí puedes forzar su detención, borrar su " +
                "caché, restringir su batería o desinstalarla.",
            fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp)
        )

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            apps.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aún no hay datos de uso de las últimas 24 h.", color = TextSecondary, fontSize = 13.sp)
            }
            else -> {
                val maxMin = apps.maxOf { it.foregroundMinutes }.coerceAtLeast(1)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        AppUsageCard(app, maxMin) { HealthAdvisor.openAppDetails(context, app.packageName) }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

private fun minutesText(m: Long): String = if (m < 60) "$m min" else "${m / 60} h ${m % 60} min"

@Composable
fun AppUsageCard(app: AppUsageInfo, maxMin: Long, onOpen: () -> Unit) {
    val barFraction = (app.foregroundMinutes.toFloat() / maxMin).coerceIn(0.02f, 1f)
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (app.isSystem) Icons.Default.Android else Icons.Default.Apps,
                        contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(app.appName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${minutesText(app.foregroundMinutes)} · usada " +
                            DateUtils.getRelativeTimeSpanString(app.lastUsed, System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS).toString().lowercase(),
                        fontSize = 12.sp, color = TextSecondary
                    )
                }
            }
            LinearProgressIndicator(
                progress = { barFraction },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = PrimaryBlue,
                trackColor = PrimaryBlue.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun HistoryTab(history: List<OptimizationRecord>, onClearHistory: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${history.size} perfiles aplicados", fontSize = 13.sp, color = TextSecondary)
            if (history.isNotEmpty()) {
                TextButton(onClick = { confirmClear = true }) { Text("Borrar historial", color = AccentRed) }
            }
        }

        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Text("Sin historial todavía", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history, key = { it.timestamp }) { record -> HistoryCard(record, fmt) }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("¿Borrar el historial?") },
            text = { Text("Solo se borra esta lista. No cambia nada del teléfono.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearHistory() }) { Text("Borrar", color = AccentRed) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun HistoryCard(record: OptimizationRecord, fmt: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(AccentGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(record.profileName, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                    Text(fmt.format(Date(record.timestamp)), fontSize = 12.sp, color = TextSecondary)
                }
                Text(
                    if (record.actionCount == 1) "1 ajuste aplicado" else "${record.actionCount} ajustes aplicados",
                    fontSize = 12.sp, color = TextSecondary
                )
            }
        }
    }
}
