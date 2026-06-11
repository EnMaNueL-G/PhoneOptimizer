package com.enmanuelgil.optimizer.ui.screens

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import com.enmanuelgil.optimizer.service.MaintenancePrefs
import com.enmanuelgil.optimizer.service.MaintenanceScheduler
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
    val context = LocalContext.current
    var monitorActive by remember { mutableStateOf(true) }
    var autoMaintEnabled by remember { mutableStateOf(MaintenancePrefs.isEnabled(context)) }
    var autoMaintInterval by remember { mutableStateOf(MaintenancePrefs.intervalHours(context)) }

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

        // Mantenimiento automático periódico
        SectionHeader("Mantenimiento Automático")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Optimización periódica automática", fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text(
                            "Mantiene el rendimiento solo: libera RAM y detiene procesos en segundo plano cada cierto tiempo, sin que hagas nada.",
                            fontSize = 12.sp, color = TextSecondary
                        )
                    }
                    Switch(
                        checked = autoMaintEnabled,
                        onCheckedChange = { on ->
                            autoMaintEnabled = on
                            MaintenancePrefs.setEnabled(context, on)
                            if (on) MaintenanceScheduler.enable(context, autoMaintInterval)
                            else MaintenanceScheduler.disable(context)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen)
                    )
                }
                if (autoMaintEnabled) {
                    Text("Frecuencia", fontSize = 12.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 6, 12, 24).forEach { h ->
                            val selected = autoMaintInterval == h
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) AccentGreen.copy(alpha = 0.18f) else BackgroundDark,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, if (selected) AccentGreen else TextSecondary.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        autoMaintInterval = h
                                        MaintenancePrefs.setIntervalHours(context, h)
                                        MaintenanceScheduler.enable(context, h)
                                    }
                            ) {
                                Text(
                                    "${h}h",
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = if (selected) AccentGreen else TextSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    Text(
                        "Cada ${autoMaintInterval} horas. Funciona en segundo plano aunque cierres la app.",
                        fontSize = 11.sp, color = AccentGreen.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Donaciones
        SectionHeader("Apoya el Proyecto")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentOrange.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(22.dp))
                    Text(
                        "¿Te fue útil la app?",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                }
                Text(
                    "PhoneOptimizer es 100% gratuita, sin anuncios y de código abierto. " +
                    "Si mejoró el rendimiento de tu dispositivo, considera apoyar su desarrollo " +
                    "con una contribución voluntaria — cada aporte ayuda a seguir mejorando la app.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                Divider(color = TextSecondary.copy(alpha = 0.1f))
                // — Binance Pay ID —
                Text("Binance Pay", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AccentOrange)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BackgroundDark)
                        .border(1.dp, AccentOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Pay ID", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "1140153333",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange
                        )
                    }
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString("1140153333")) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar Pay ID", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    "Abre Binance → Pagar → Buscar → Pegar Pay ID",
                    fontSize = 11.sp,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                // — BSC BEP20 —
                Text("Cripto directo — BSC BEP20", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AccentOrange)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BackgroundDark)
                        .border(1.dp, AccentOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Binance Smart Chain", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "0x0a9a0d8d816ede885d1d4a5c94369a72ef86b3c1",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange
                        )
                    }
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString("0x0a9a0d8d816ede885d1d4a5c94369a72ef86b3c1")) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar dirección BSC", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    "Compatible con BNB, USDT, USDC y cualquier token BEP20",
                    fontSize = 11.sp,
                    color = TextSecondary.copy(alpha = 0.7f)
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(
                            id = com.enmanuelgil.optimizer.R.drawable.brand_logo),
                        contentDescription = "OptiSuite",
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    )
                    Column {
                        Text("PhoneOptimizer", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                        Text("Un proyecto OptiSuite · 100% gratis", fontSize = 12.sp, color = AccentGreen)
                    }
                }
                Divider(color = TextSecondary.copy(alpha = 0.1f))
                InfoRow("Versión", "1.6.0")
                InfoRow("Desarrollado por", "Enmanuel Gil")
                InfoRow("Compatibilidad", "Android 8.0+ (API 26)")
                InfoRow("Sin anuncios ni telemetría", "Gratis para siempre · sin root")
                Divider(color = TextSecondary.copy(alpha = 0.1f))
                Text(
                    "Optimizador integral para Android: mantiene RAM, CPU y temperatura en óptimo " +
                    "rendimiento en tiempo real, con mantenimiento automático periódico. No elimina " +
                    "datos personales ni modifica archivos del usuario.",
                    fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp
                )
                // Contacto / web
                val ctx2 = LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            try {
                                ctx2.startActivity(Intent(Intent.ACTION_SENDTO,
                                    android.net.Uri.parse("mailto:support@optisuite.app?subject=PhoneOptimizer")))
                            } catch (_: Exception) {}
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Text("support@optisuite.app", fontSize = 13.sp, color = PrimaryBlue, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Text("optisuite.app", fontSize = 13.sp, color = TextSecondary)
                }
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
