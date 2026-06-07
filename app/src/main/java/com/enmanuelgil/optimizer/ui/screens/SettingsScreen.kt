package com.enmanuelgil.optimizer.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enmanuelgil.optimizer.ui.theme.*
import com.enmanuelgil.optimizer.viewmodel.PrivilegesStatus

@Composable
fun SettingsScreen(
    privilegesStatus: PrivilegesStatus,
    adBlockEnabled: Boolean,
    onAdBlockToggle: (Boolean) -> Unit,
    onStartMonitor: () -> Unit,
    onStopMonitor: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var monitorActive by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Configuración", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        // Estado de permisos avanzados
        SectionHeader("Estado de Optimización Avanzada")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (privilegesStatus) {
                    PrivilegesStatus.GRANTED -> {
                        StatusRow(Icons.Default.CheckCircle, "Permisos avanzados activos", AccentGreen)
                        Text(
                            "Animaciones, WiFi scan, Doze y sincronización son controlables. " +
                            "El comando ADB ya fue ejecutado correctamente.",
                            fontSize = 13.sp, color = TextSecondary
                        )
                    }
                    else -> {
                        StatusRow(Icons.Default.Warning, "Solo optimización básica activa", AccentOrange)
                        Text(
                            "RAM y procesos funcionan sin configuración extra.\n" +
                            "Para desbloquear animaciones, WiFi scan, Doze y más: " +
                            "ejecuta el comando de abajo una sola vez desde un PC con ADB.",
                            fontSize = 13.sp, color = TextSecondary
                        )
                    }
                }
            }
        }

        // Comando ADB de activación
        SectionHeader("Activación Avanzada — Un Solo Comando ADB")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Conecta el teléfono al PC con USB, abre una terminal y ejecuta:",
                    fontSize = 13.sp, color = TextSecondary
                )
                AdbCommandBox(
                    label = "Paso único — copiar y ejecutar en PC:",
                    command = "adb shell pm grant com.enmanuelgil.optimizer android.permission.WRITE_SECURE_SETTINGS",
                    onCopy = { clipboard.setText(AnnotatedString(it)) }
                )
                Text("Después de ejecutar, cierra y vuelve a abrir la app.", fontSize = 12.sp, color = AccentOrange)

                Divider(color = TextSecondary.copy(alpha = 0.1f))

                // Aviso MIUI
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(16.dp).padding(top = 1.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Xiaomi con MIUI V14 / HyperOS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentOrange
                        )
                        Text(
                            "El fabricante bloquea este comando con el error:\n" +
                            "\"GRANT_RUNTIME_PERMISSIONS not allowed\"\n\n" +
                            "No es un fallo de la app — es una restricción de Xiaomi. " +
                            "La optimización básica (RAM, procesos, GC) funciona igual sin el comando. " +
                            "Las funciones avanzadas (animaciones, WiFi scan, Doze) no están disponibles en este modelo.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            "Dispositivos confirmados SIN este problema: Samsung Galaxy (todos), Motorola, Google Pixel, OnePlus.",
                            fontSize = 11.sp,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Bloqueo de anuncios DNS
        SectionHeader("Bloqueo de Anuncios — DNS Privado")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Bloquear anuncios del sistema", fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text(
                            if (adBlockEnabled) "Activo — DNS: dns.adguard.com" else "Inactivo",
                            fontSize = 12.sp,
                            color = if (adBlockEnabled) AccentGreen else TextSecondary
                        )
                    }
                    Switch(
                        checked = adBlockEnabled,
                        onCheckedChange = onAdBlockToggle,
                        enabled = privilegesStatus == PrivilegesStatus.GRANTED,
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen)
                    )
                }
                Text(
                    "Redirige las consultas DNS al servidor de AdGuard. Bloquea anuncios y rastreadores en todas las apps sin instalar nada extra. Requiere permisos avanzados (ADB).",
                    fontSize = 12.sp, color = TextSecondary
                )
                if (privilegesStatus != PrivilegesStatus.GRANTED) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(14.dp))
                        Text("Requiere el comando ADB de abajo", fontSize = 11.sp, color = AccentOrange)
                    }
                }
            }
        }

        // Monitor en background
        SectionHeader("Monitor Térmico en Background")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Monitoreo continuo", fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text(
                        "Alerta y optimiza automáticamente al detectar sobrecalentamiento",
                        fontSize = 12.sp, color = TextSecondary
                    )
                }
                Switch(
                    checked = monitorActive,
                    onCheckedChange = { active ->
                        monitorActive = active
                        if (active) onStartMonitor() else onStopMonitor()
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue)
                )
            }
        }

        // Info de la app
        SectionHeader("Acerca de PhoneOptimizer")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow("Versión", "1.1.0")
                InfoRow("Desarrollado por", "Enmanuel Gil")
                InfoRow("Compatibilidad", "Android 8.0+ (API 26)")
                InfoRow("Sin dependencias externas", "No requiere Shizuku ni root")
                Divider(color = TextSecondary.copy(alpha = 0.1f))
                Text(
                    "PhoneOptimizer optimiza CPU, RAM y temperatura en tiempo real. " +
                    "No elimina datos personales ni modifica archivos del usuario.",
                    fontSize = 12.sp, color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
}

@Composable
fun StatusRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, color = color, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun AdbCommandBox(label: String, command: String, onCopy: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .border(1.dp, TextSecondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                command, fontSize = 11.sp, color = AccentGreen,
                fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onCopy(command) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
