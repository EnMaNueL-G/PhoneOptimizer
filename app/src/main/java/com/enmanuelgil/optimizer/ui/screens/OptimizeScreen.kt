package com.enmanuelgil.optimizer.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
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
    canRestore: Boolean,
    onRestore: () -> Unit,
    onOptimize: () -> Unit,
    onCloseBackground: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Optimizar", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        PrivilegesStatusCard(privilegesStatus)

        Text("Elige un perfil", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
        OptimizationProfile.entries.forEach { profile ->
            ProfileCard(
                profile = profile,
                selected = selectedProfile == profile,
                onClick = { onProfileSelected(profile) }
            )
        }

        Button(
            onClick = onOptimize,
            enabled = !isOptimizing && privilegesStatus == PrivilegesStatus.GRANTED,
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
                Text(if (privilegesStatus == PrivilegesStatus.GRANTED) "Aplicar perfil" else "Requiere modo avanzado",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        AnimatedVisibility(visible = lastResult != null) {
            lastResult?.let { ResultCard(it) }
        }

        ProfileDetailCard(selectedProfile, privilegesStatus)

        if (canRestore) {
            OutlinedButton(
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Restaurar animaciones y ajustes originales")
            }
        }

        if (com.enmanuelgil.optimizer.core.OptimizationEngine.canCloseBackgroundApps) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cerrar apps en segundo plano", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(
                        "Disponible en tu versión de Android (13 o anterior). Pide al sistema cerrar las apps " +
                        "que no estás usando; algunos fabricantes lo ignoran y Android puede volver a abrirlas. " +
                        "Útil antes de un juego pesado; no hace falta a diario.",
                        fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp
                    )
                    OutlinedButton(
                        onClick = onCloseBackground,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) { Text("Cerrar apps en segundo plano") }
                }
            }
        }

        Text(
            "Lo que NO puede hacer ninguna app sin root (aunque lo prometan): cerrar otras apps en Android 14+, borrar " +
            "su caché, \"enfriar\" el procesador o aumentar la RAM. Lo que más ayuda de verdad: espacio " +
            "libre, reiniciar cada semana y no usar el teléfono mientras carga.",
            fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun PrivilegesStatusCard(status: PrivilegesStatus) {
    val granted = status == PrivilegesStatus.GRANTED
    val color = if (granted) AccentGreen else AccentOrange
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
            Icon(if (granted) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f)) {
                Text(if (granted) "Modo avanzado activo" else "Modo básico",
                    fontWeight = FontWeight.SemiBold, color = color)
                Text(
                    if (granted) "Puedes aplicar los perfiles y restaurarlos cuando quieras."
                    else "Para cambiar animaciones y búsqueda WiFi, actívalo una vez desde un PC (Ajustes → Modo avanzado). " +
                        "Mientras tanto, el Panel te dice qué mejorar a mano.",
                    fontSize = 12.sp, color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun ProfileCard(profile: OptimizationProfile, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        colors = CardDefaults.cardColors(containerColor = if (selected) PrimaryBlue.copy(alpha = 0.12f) else CardDark),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) PrimaryBlue else CardDark),
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
                },
                contentDescription = null,
                tint = if (selected) PrimaryBlue else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(profile.displayName,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = TextPrimary)
                Text(profile.description, fontSize = 12.sp, color = TextSecondary)
            }
            RadioButton(selected = selected, onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue))
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
                Icon(if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                Text(if (result.success) "Hecho" else (result.errorMessage ?: "Error"),
                    fontWeight = FontWeight.Bold, color = color)
            }
            if (result.actionsTaken.isNotEmpty()) {
                result.actionsTaken.forEach { action ->
                    val failed = action.startsWith("✗")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                        Text(if (failed) "✗" else "•", color = if (failed) AccentOrange else color, fontSize = 13.sp)
                        Text(action.removePrefix("✗ "), fontSize = 13.sp, color = TextSecondary)
                    }
                }
                Text(
                    "Se nota al abrir apps y cambiar de pantalla. Puedes deshacerlo con \"Restaurar\".",
                    fontSize = 12.sp, color = TextSecondary
                )
            }
        }
    }
}


@Composable
fun ProfileDetailCard(profile: OptimizationProfile, privilegesStatus: PrivilegesStatus) {
    val advanced = privilegesStatus == PrivilegesStatus.GRANTED
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Qué hace \"${profile.displayName}\"", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            ProfileFeature(
                if (profile.animationScale == 0f) "Quitar animaciones" else "Animaciones al doble de rápido",
                enabled = true, available = advanced
            )
            ProfileFeature("No buscar redes WiFi/Bluetooth con el WiFi apagado",
                enabled = profile.disableWifiScan, available = advanced)
            Text(
                "No toca tus apps ni tus datos. Los ajustes cambiados se pueden restaurar cuando quieras.",
                fontSize = 12.sp, color = TextSecondary
            )
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
        Text(name, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        if (!available && enabled) {
            Text("Requiere modo avanzado", fontSize = 11.sp, color = AccentOrange)
        } else {
            Icon(
                if (enabled) Icons.Default.CheckCircle else Icons.Default.RemoveCircleOutline,
                contentDescription = if (enabled) "Sí" else "No",
                tint = if (enabled) AccentGreen else TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
