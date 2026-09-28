package com.enmanuelgil.optimizer.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.optimizer.core.HealthAdvisor
import com.enmanuelgil.optimizer.model.DeviceStats
import com.enmanuelgil.optimizer.model.ThermalStatus
import com.enmanuelgil.optimizer.ui.theme.*

@Composable
fun DashboardScreen(
    stats: DeviceStats,
    onGoOptimize: () -> Unit = {}
) {
    val loaded = stats.ramTotalMb > 0
    val tips = remember(stats) { if (loaded) HealthAdvisor.check(stats) else emptyList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Panel", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            if (loaded) ThermalBadge(stats.thermalStatus)
        }

        // Métricas principales
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularMetric(
                modifier = Modifier.weight(1f),
                label = "RAM en uso",
                value = stats.ramUsagePercent, maxValue = 100f, unit = "%",
                color = when {
                    stats.ramUsagePercent > 90f -> AccentOrange
                    else -> PrimaryBlue
                }
            )
            CircularMetric(
                modifier = Modifier.weight(1f),
                label = "Almacenam.",
                value = stats.storageUsagePercent, maxValue = 100f, unit = "%",
                color = when {
                    stats.storageUsagePercent >= 90f -> AccentRed
                    stats.storageUsagePercent >= 80f -> AccentOrange
                    else -> AccentGreen
                }
            )
            val t = stats.temperatureBattery
            CircularMetric(
                modifier = Modifier.weight(1f),
                label = "Batería °C",
                value = t, maxValue = 60f, unit = "°C",
                color = when {
                    t >= 45f -> AccentRed
                    t >= 40f -> AccentOrange
                    else -> AccentGreen
                }
            )
        }

        // Recomendaciones
        if (loaded) RecommendationsCard(tips)

        // Detalle
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Memory,
                label = "RAM disponible",
                value = "${stats.ramAvailableMb} MB",
                subValue = "de ${stats.ramTotalMb} MB"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = if (stats.batteryCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                label = "Batería",
                value = "${stats.batteryLevel} %",
                subValue = if (stats.batteryCharging) "Cargando" else "Sin cargar"
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Storage,
                label = "Almacenamiento",
                value = "${"%.1f".format(stats.storageTotalGb - stats.storageUsedGb)} GB",
                subValue = "libres de ${"%.0f".format(stats.storageTotalGb)}"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.RestartAlt,
                label = "Sin reiniciar",
                value = uptimeText(stats.uptimeHours),
                subValue = "encendido"
            )
        }
        if (stats.temperatureCpu > 0f || stats.cpuAvailable) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (stats.temperatureCpu > 0f) StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DeviceThermostat,
                    label = "Procesador",
                    value = "${"%.0f".format(stats.temperatureCpu)} °C",
                    subValue = "temperatura"
                )
                if (stats.cpuAvailable) StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    label = "Uso de CPU",
                    value = "${stats.cpuUsagePercent.toInt()} %",
                    subValue = "ahora"
                )
            }
        }

        // Swap (memoria comprimida)
        if (stats.swapTotalMb > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Memoria comprimida (swap)", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { stats.swapUsedMb.toFloat() / stats.swapTotalMb },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = PrimaryBlue,
                        trackColor = BackgroundDark
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Usada: ${stats.swapUsedMb} MB", color = TextSecondary, fontSize = 12.sp)
                        Text("Total: ${stats.swapTotalMb} MB", color = TextSecondary, fontSize = 12.sp)
                    }
                    Text(
                        "Android comprime en memoria las apps que no usas. Que esté ocupada es normal.",
                        color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        GoOptimizeButton(onClick = onGoOptimize)

        Text(
            "Nota: que la RAM esté casi llena es normal en Android — el sistema la usa para abrir " +
            "las apps más rápido y la libera solo cuando hace falta.",
            fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp
        )

        Spacer(Modifier.height(24.dp))
    }
}

private fun uptimeText(hours: Long): String = when {
    hours < 1 -> "< 1 hora"
    hours < 48 -> "$hours h"
    else -> "${hours / 24} días"
}

@Composable
fun RecommendationsCard(tips: List<HealthAdvisor.Tip>) {
    val context = LocalContext.current
    var showRestartHelp by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tips.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(24.dp))
                    Column {
                        Text("Todo en orden", fontWeight = FontWeight.SemiBold, color = AccentGreen)
                        Text("Espacio, temperatura y batería están bien.", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            } else {
                Text("Recomendaciones", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                tips.forEach { tip ->
                    val color = when (tip.level) {
                        HealthAdvisor.Level.ALERT -> AccentRed
                        HealthAdvisor.Level.WARN -> AccentOrange
                        HealthAdvisor.Level.INFO -> PrimaryBlue
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            if (tip.level == HealthAdvisor.Level.INFO) Icons.Default.Info else Icons.Default.Warning,
                            contentDescription = null, tint = color,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(tip.title, fontWeight = FontWeight.SemiBold, color = color, fontSize = 14.sp)
                            Text(tip.detail, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                            if (tip.action != HealthAdvisor.Action.NONE) {
                                TextButton(
                                    onClick = {
                                        if (tip.action == HealthAdvisor.Action.RESTART_HELP) showRestartHelp = true
                                        else HealthAdvisor.open(context, tip.action)
                                    },
                                    contentPadding = PaddingValues(horizontal = 0.dp)
                                ) { Text(tip.action.label, color = PrimaryBlue) }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showRestartHelp) {
        AlertDialog(
            onDismissRequest = { showRestartHelp = false },
            confirmButton = { TextButton(onClick = { showRestartHelp = false }) { Text("Entendido") } },
            title = { Text("Reiniciar el teléfono") },
            text = {
                Text("Mantén pulsado el botón de encendido (en algunos modelos, encendido + subir volumen) " +
                    "y elige \"Reiniciar\". Tarda un minuto y no borra nada.")
            }
        )
    }
}

@Composable
fun GoOptimizeButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(AccentGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Hacerlo más ágil", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                Text("Animaciones más rápidas y ahorro de batería", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun ThermalBadge(status: ThermalStatus) {
    val color = Color(status.color)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics { contentDescription = "Estado térmico: ${status.label}" }
    ) {
        Text("Térmico: ${status.label.lowercase()}", color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CircularMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: Float,
    maxValue: Float,
    unit: String,
    color: Color
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(0f, maxValue),
        animationSpec = tween(600), label = "metric"
    )
    val shown = if (unit == "°C") "${"%.0f".format(value)}°" else "${"%.0f".format(value)}"
    Card(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$label: ${"%.0f".format(value)} $unit" },
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(76.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    drawArc(color = color.copy(alpha = 0.2f), startAngle = 135f, sweepAngle = 270f,
                        useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                    drawArc(color = color, startAngle = 135f, sweepAngle = 270f * (animatedValue / maxValue),
                        useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(shown, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
                    if (unit != "°C") Text(unit, fontSize = 11.sp, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String
) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(label, fontSize = 12.sp, color = TextSecondary)
                Text(subValue, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}
