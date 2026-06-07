package com.enmanuelgil.optimizer.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.optimizer.model.OptimizationProfile
import com.enmanuelgil.optimizer.model.OptimizationResult
import com.enmanuelgil.optimizer.ui.theme.*
import com.enmanuelgil.optimizer.viewmodel.PrivilegesStatus

@Composable
fun OptimizeScreen(
    selectedProfile: OptimizationProfile,
    onProfileSelected: (OptimizationProfile) -> Unit,
    isOptimizing: Boolean,
    progress: String,
    lastResult: OptimizationResult?,
    privilegesStatus: PrivilegesStatus,
    onOptimize: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Optimización", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        // Estado de permisos avanzados
        PrivilegesStatusCard(privilegesStatus)

        // Selector de perfil
        Text("Perfil de Optimización", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
        OptimizationProfiles.forEach { profile ->
            ProfileCard(
                profile = profile,
                selected = selectedProfile == profile,
                onClick = { onProfileSelected(profile) }
            )
        }

        Spacer(Modifier.height(4.dp))

        // Botón principal
        Button(
            onClick = onOptimize,
            enabled = !isOptimizing,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue,
                disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f)
            )
        ) {
            if (isOptimizing) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(progress.ifEmpty { "Optimizando..." }, color = Color.White, fontSize = 15.sp)
            } else {
                Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Optimizar Ahora", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Resultado
        AnimatedVisibility(visible = lastResult != null) {
            lastResult?.let { ResultCard(it) }
        }

        // Detalle del perfil seleccionado
        ProfileDetailCard(selectedProfile, privilegesStatus)

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun PrivilegesStatusCard(status: PrivilegesStatus) {
    val color = if (status == PrivilegesStatus.GRANTED) AccentGreen else AccentOrange
    val icon = if (status == PrivilegesStatus.GRANTED) Icons.Default.CheckCircle else Icons.Default.Warning
    val title = if (status == PrivilegesStatus.GRANTED)
        "Optimización completa activa"
    else
        "Modo básico — sin configuración extra"
    val message = if (status == PrivilegesStatus.GRANTED)
        "Animaciones, WiFi scan, Doze y más disponibles"
    else
        "RAM y procesos funcionan. Ve a Ajustes para activar funciones avanzadas"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = color)
                Text(message, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun ProfileCard(profile: OptimizationProfile, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) PrimaryBlue else CardDark
    val bgColor = if (selected) PrimaryBlue.copy(alpha = 0.12f) else CardDark

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                when (profile) {
                    OptimizationProfile.RECOMMENDED -> Icons.Default.Stars
                    OptimizationProfile.PERFORMANCE -> Icons.Default.Speed
                    OptimizationProfile.BATTERY_SAVER -> Icons.Default.BatteryFull
                    OptimizationProfile.THERMAL_GUARD -> Icons.Default.AcUnit
                    OptimizationProfile.CUSTOM -> Icons.Default.Tune
                },
                contentDescription = null,
                tint = if (selected) PrimaryBlue else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    profile.displayName,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) TextPrimary else TextSecondary
                )
                Text(profile.description, fontSize = 12.sp, color = TextSecondary.copy(alpha = 0.8f))
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ResultCard(result: OptimizationResult) {
    val color = if (result.success) AccentGreen else AccentRed
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null, tint = color, modifier = Modifier.size(22.dp)
                )
                Text(
                    if (result.success) "Optimización completada" else "Error",
                    fontWeight = FontWeight.Bold, color = color
                )
            }
            if (result.success) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ResultStat("Apps detenidas", "${result.appsKilled}")
                    ResultStat("RAM liberada", "${result.ramFreedMb} MB")
                    ResultStat("Acciones", "${result.actionsTaken.size}")
                }
                result.actionsTaken.forEach { action ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                        Text("•", color = color, fontSize = 13.sp)
                        Text(action, fontSize = 13.sp, color = TextSecondary)
                    }
                }
            } else {
                Text(result.errorMessage ?: "Error desconocido", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ResultStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
fun ProfileDetailCard(profile: OptimizationProfile, privilegesStatus: PrivilegesStatus) {
    if (profile == OptimizationProfile.CUSTOM) return
    val advanced = privilegesStatus == PrivilegesStatus.GRANTED
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Detalle del perfil: ${profile.displayName}", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            ProfileFeature("Reducir animaciones", profile.reducedAnimations, advanced)
            ProfileFeature("Restringir datos background", profile.restrictBackgroundData, advanced)
            ProfileFeature("Desactivar WiFi scan pasivo", profile.disableWifiScan, advanced)
            ProfileFeature("Doze profundo", profile.enableDoze, advanced)
            ProfileFeature("Pausar sincronización", profile.pauseSync, advanced)
            ProfileFeature("Auto-optimizar al calentar", profile.autoOptimizeOnHeat, true)
        }
    }
}

@Composable
fun ProfileFeature(name: String, enabled: Boolean, available: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 13.sp, color = if (available) TextSecondary else TextSecondary.copy(alpha = 0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!available && enabled) {
                Text("Requiere setup ADB", fontSize = 10.sp, color = AccentOrange)
            }
            Icon(
                if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = when {
                    !enabled -> TextSecondary.copy(alpha = 0.4f)
                    available -> AccentGreen
                    else -> AccentOrange
                },
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private val OptimizationProfiles = listOf(
    OptimizationProfile.RECOMMENDED,
    OptimizationProfile.PERFORMANCE,
    OptimizationProfile.BATTERY_SAVER,
    OptimizationProfile.THERMAL_GUARD,
    OptimizationProfile.CUSTOM
)
