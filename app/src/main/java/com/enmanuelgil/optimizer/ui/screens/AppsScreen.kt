package com.enmanuelgil.optimizer.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.optimizer.core.AppMemoryInfo
import com.enmanuelgil.optimizer.model.OptimizationRecord
import com.enmanuelgil.optimizer.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppsScreen(
    topApps: List<AppMemoryInfo>,
    isLoading: Boolean,
    history: List<OptimizationRecord>,
    onRefreshApps: () -> Unit,
    onForceStop: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { onRefreshApps() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Top Apps", "Historial").forEachIndexed { index, label ->
                val selected = selectedTab == index
                FilterChip(
                    selected = selected,
                    onClick = { selectedTab = index },
                    label = { Text(label, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f),
                        selectedLabelColor = PrimaryBlue
                    )
                )
            }
        }

        when (selectedTab) {
            0 -> TopAppsTab(topApps, isLoading, onRefreshApps, onForceStop)
            1 -> HistoryTab(history, onClearHistory)
        }
    }
}

@Composable
fun TopAppsTab(
    apps: List<AppMemoryInfo>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onForceStop: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Apps por uso de RAM", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = PrimaryBlue)
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            return
        }

        if (apps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay apps en ejecución detectadas", color = TextSecondary, fontSize = 13.sp)
            }
            return
        }

        val maxMem = apps.maxOf { it.memoryMb }.coerceAtLeast(1)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(apps) { app ->
                AppMemoryCard(app, maxMem, onForceStop)
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun AppMemoryCard(app: AppMemoryInfo, maxMem: Int, onForceStop: (String) -> Unit) {
    val barFraction = (app.memoryMb.toFloat() / maxMem).coerceIn(0.02f, 1f)
    val barColor = when {
        app.memoryMb > 300 -> AccentRed
        app.memoryMb > 150 -> AccentOrange
        else -> PrimaryBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(barColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (app.isSystem) Icons.Default.Android else Icons.Default.Apps,
                            contentDescription = null,
                            tint = barColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            app.appName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            app.packageName.substringAfterLast("."),
                            fontSize = 11.sp,
                            color = TextSecondary.copy(alpha = 0.6f)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${app.memoryMb} MB",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                    if (!app.isSystem) {
                        IconButton(
                            onClick = { onForceStop(app.packageName) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Detener",
                                tint = AccentRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            LinearProgressIndicator(
                progress = { barFraction },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = barColor,
                trackColor = barColor.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun HistoryTab(history: List<OptimizationRecord>, onClearHistory: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${history.size} optimizaciones registradas",
                fontSize = 13.sp,
                color = TextSecondary
            )
            if (history.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text("Limpiar", color = AccentRed, fontSize = 12.sp)
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                    Text("Sin historial todavía", color = TextSecondary, fontSize = 14.sp)
                    Text("Optimiza el dispositivo para ver los registros aquí", color = TextSecondary.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) { record ->
                HistoryCard(record, fmt)
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
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
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AccentGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(record.profileName, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                    Text(fmt.format(Date(record.timestamp)), fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.6f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HistoryStat("RAM", "+${record.ramFreedMb} MB")
                    HistoryStat("Apps", "${record.appsKilled}")
                    HistoryStat("Acciones", "${record.actionCount}")
                    if (record.temperatureBefore > 0) {
                        HistoryStat("Temp", "${record.temperatureBefore.toInt()}°C")
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
        Text(label, fontSize = 10.sp, color = TextSecondary.copy(alpha = 0.6f))
    }
}
