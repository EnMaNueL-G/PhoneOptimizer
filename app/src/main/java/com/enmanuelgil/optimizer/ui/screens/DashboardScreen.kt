package com.enmanuelgil.optimizer.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.Canvas
import com.enmanuelgil.optimizer.model.DeviceStats
import com.enmanuelgil.optimizer.model.ThermalStatus
import com.enmanuelgil.optimizer.ui.theme.*

@Composable
fun DashboardScreen(stats: DeviceStats) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Panel de Control", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            ThermalBadge(stats.thermalStatus)
        }

        // Estado térmico destacado si hay problema
        if (stats.thermalStatus != ThermalStatus.NONE) {
            ThermalWarningCard(stats)
        }

        // Métricas principales
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularMetric(
                modifier = Modifier.weight(1f),
                label = "CPU",
                value = stats.cpuUsagePercent,
                maxValue = 100f,
                unit = "%",
                color = when {
                    stats.cpuUsagePercent > 80f -> AccentRed
                    stats.cpuUsagePercent > 60f -> AccentOrange
                    else -> AccentGreen
                }
            )
            CircularMetric(
                modifier = Modifier.weight(1f),
                label = "RAM",
                value = stats.ramUsagePercent,
                maxValue = 100f,
                unit = "%",
                color = when {
                    stats.ramUsagePercent > 85f -> AccentRed
                    stats.ramUsagePercent > 70f -> AccentOrange
                    else -> PrimaryBlue
                }
            )
            CircularMetric(
                modifier = Modifier.weight(1f),
                label = "Temp",
                value = stats.temperatureCpu,
                maxValue = 60f,
                unit = "°C",
                color = when {
                    stats.temperatureCpu > 50f -> AccentRed
                    stats.temperatureCpu > 42f -> AccentOrange
                    else -> AccentGreen
                }
            )
        }

        // Tarjetas de detalle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Memory,
                label = "RAM Libre",
                value = "${stats.ramAvailableMb} MB",
                subValue = "de ${stats.ramTotalMb} MB"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.BatteryChargingFull,
                label = "Batería",
                value = "${stats.batteryLevel}%",
                subValue = if (stats.batteryCharging) "Cargando" else "Descargando"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Storage,
                label = "Almacenamiento",
                value = "${"%.1f".format(stats.storageUsedGb)} GB",
                subValue = "de ${"%.1f".format(stats.storageTotalGb)} GB"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Apps,
                label = "Procesos",
                value = "${stats.runningProcesses}",
                subValue = "activos"
            )
        }

        // Swap
        if (stats.swapTotalMb > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Memoria Virtual (Swap)", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (stats.swapTotalMb > 0) (stats.swapUsedMb.toFloat() / stats.swapTotalMb) else 0f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (stats.swapUsedMb > stats.swapTotalMb * 0.7f) AccentOrange else PrimaryBlue,
                        trackColor = CardDark.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Usado: ${stats.swapUsedMb} MB", color = TextSecondary, fontSize = 12.sp)
                        Text("Total: ${stats.swapTotalMb} MB", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp)) // espacio para bottom nav
    }
}

@Composable
fun ThermalWarningCard(stats: DeviceStats) {
    val color = Color(stats.thermalStatus.color)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Column {
                Text("Temperatura ${stats.thermalStatus.label}", fontWeight = FontWeight.Bold, color = color)
                Text(
                    "CPU: ${"%.1f".format(stats.temperatureCpu)}°C | Superficie: ${"%.1f".format(stats.temperatureSkin)}°C",
                    color = TextSecondary, fontSize = 13.sp
                )
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
    ) {
        Text(status.label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    drawArc(
                        color = color.copy(alpha = 0.2f),
                        startAngle = 135f, sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = color,
                        startAngle = 135f,
                        sweepAngle = 270f * (animatedValue / maxValue),
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (unit == "°C") "${"%.0f".format(animatedValue)}°" else "${"%.0f".format(animatedValue)}",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color
                    )
                    Text(unit, fontSize = 10.sp, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
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
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(label, fontSize = 11.sp, color = TextSecondary)
                Text(subValue, fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.7f))
            }
        }
    }
}
