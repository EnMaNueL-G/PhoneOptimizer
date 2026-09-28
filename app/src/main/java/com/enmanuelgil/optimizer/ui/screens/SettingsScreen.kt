package com.enmanuelgil.optimizer.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.Role
import androidx.core.content.ContextCompat
import com.enmanuelgil.optimizer.BuildConfig
import com.enmanuelgil.optimizer.core.AdBlockManager
import com.enmanuelgil.optimizer.service.MonitorPrefs
import com.enmanuelgil.optimizer.service.ThermalMonitorService
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
    canRestore: Boolean,
    onAdBlockToggle: (Boolean) -> Unit,
    onRestore: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val granted = privilegesStatus == PrivilegesStatus.GRANTED
    var monitorActive by rememberSaveable { mutableStateOf(MonitorPrefs.isEnabled(context)) }
    var autoMaintEnabled by rememberSaveable { mutableStateOf(MaintenancePrefs.isEnabled(context)) }
    var autoMaintInterval by rememberSaveable { mutableStateOf(MaintenancePrefs.intervalHours(context)) }

    // Android 13+: las alertas necesitan el permiso de notificaciones
    fun needsNotifPermission() = Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
    var notifDenied by remember { mutableStateOf(false) }
    var pendingEnable by remember { mutableStateOf<String?>(null) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        notifDenied = !ok
        when (pendingEnable) {
            "monitor" -> { MonitorPrefs.setEnabled(context, true); monitorActive = true; ThermalMonitorService.start(context) }
            "checkup" -> {
                MaintenancePrefs.setEnabled(context, true); autoMaintEnabled = true
                MaintenanceScheduler.enable(context, autoMaintInterval)
            }
        }
        pendingEnable = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ajustes", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        if (notifDenied) {
            Text(
                "Sin permiso de notificaciones no verás los avisos. Puedes darlo en Ajustes de Android → Apps → PhoneOptimizer → Notificaciones.",
                fontSize = 12.sp, color = AccentOrange
            )
        }

        // Monitor de temperatura
        SectionHeader("Monitor de temperatura")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Avisarme si se calienta", fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text(
                        "Revisa la temperatura cada minuto con la pantalla encendida y te avisa con consejos " +
                        "si la batería pasa de ${ThermalMonitorService.BATTERY_ALERT_C.toInt()} °C. Deja un aviso fijo " +
                        "discreto mientras está activo. Gasto de batería mínimo.",
                        fontSize = 12.sp, color = TextSecondary
                    )
                }
                Switch(
                    checked = monitorActive,
                    onCheckedChange = { on ->
                        if (on && needsNotifPermission()) {
                            pendingEnable = "monitor"
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            monitorActive = on
                            MonitorPrefs.setEnabled(context, on)
                            if (on) ThermalMonitorService.start(context) else ThermalMonitorService.stop(context)
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = androidx.compose.ui.graphics.Color.White, checkedTrackColor = PrimaryBlue)
                )
            }
        }

        // Revisión automática
        SectionHeader("Revisión automática")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Revisar el teléfono periódicamente", fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text(
                            "Te avisa solo si encuentra algo: almacenamiento casi lleno, muchos días sin reiniciar " +
                            "o problemas de batería. No cierra apps ni cambia nada por su cuenta.",
                            fontSize = 12.sp, color = TextSecondary
                        )
                    }
                    Switch(
                        checked = autoMaintEnabled,
                        onCheckedChange = { on ->
                            if (on && needsNotifPermission()) {
                                pendingEnable = "checkup"
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                autoMaintEnabled = on
                                MaintenancePrefs.setEnabled(context, on)
                                if (on) MaintenanceScheduler.enable(context, autoMaintInterval)
                                else MaintenanceScheduler.disable(context)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = androidx.compose.ui.graphics.Color.White, checkedTrackColor = AccentGreen)
                    )
                }
                if (autoMaintEnabled) {
                    Text("Frecuencia", fontSize = 12.sp, color = TextSecondary)
                    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(12 to "12 h", 24 to "1 día", 72 to "3 días").forEach { (h, label) ->
                            val selected = autoMaintInterval == h
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) AccentGreen.copy(alpha = 0.18f) else BackgroundDark,
                                border = BorderStroke(1.dp, if (selected) AccentGreen else TextSecondary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .selectable(selected = selected, role = Role.RadioButton) {
                                        autoMaintInterval = h
                                        MaintenancePrefs.setIntervalHours(context, h)
                                        MaintenanceScheduler.enable(context, h)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        label,
                                        color = if (selected) AccentGreen else TextSecondary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modo avanzado (permiso por ADB)
        SectionHeader("Modo avanzado")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (granted) {
                    StatusRow(Icons.Default.CheckCircle, "Modo avanzado activo", AccentGreen)
                    Text(
                        "La app puede ajustar las animaciones, la búsqueda WiFi/Bluetooth y el bloqueo de anuncios.",
                        fontSize = 13.sp, color = TextSecondary
                    )
                } else {
                    StatusRow(Icons.Default.Info, "Modo avanzado desactivado", AccentOrange)
                    Text(
                        "Permite ajustar animaciones, búsqueda WiFi/Bluetooth y el bloqueo de anuncios. " +
                        "Se activa una sola vez desde un PC: activa la \"Depuración USB\" en Opciones de " +
                        "desarrollador, conecta el cable y ejecuta:",
                        fontSize = 13.sp, color = TextSecondary
                    )
                    val cmd = "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
                    AdbCommandBox(
                        label = "Comando (tócalo para copiarlo):",
                        command = cmd,
                        onCopy = { clipboard.setText(AnnotatedString(it)) }
                    )
                    Text(
                        "Xiaomi / Redmi / POCO: activa además \"Depuración USB (ajustes de seguridad)\" en " +
                        "Opciones de desarrollador; si no, el comando da el error " +
                        "\"GRANT_RUNTIME_PERMISSIONS\".",
                        fontSize = 12.sp, color = TextSecondary
                    )
                    Text(
                        "Para quitarlo: el mismo comando cambiando \"grant\" por \"revoke\".",
                        fontSize = 12.sp, color = TextSecondary
                    )
                }
                if (canRestore) {
                    OutlinedButton(
                        onClick = onRestore,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) { Text("Restaurar animaciones y ajustes originales") }
                }
            }
        }

        // Bloqueo de anuncios DNS
        SectionHeader("Bloqueo de anuncios (DNS privado)")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Bloquear anuncios y rastreadores", fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text(
                            if (adBlockEnabled) "Activo — ${AdBlockManager.ADBLOCK_DNS}" else "Inactivo",
                            fontSize = 12.sp,
                            color = if (adBlockEnabled) AccentGreen else TextSecondary
                        )
                    }
                    Switch(
                        checked = adBlockEnabled,
                        onCheckedChange = onAdBlockToggle,
                        enabled = granted,
                        colors = SwitchDefaults.colors(checkedThumbColor = androidx.compose.ui.graphics.Color.White, checkedTrackColor = AccentGreen)
                    )
                }
                Text(
                    "Usa el DNS privado de Android con AdGuard DNS: bloquea muchos anuncios dentro de apps y webs. " +
                    "No quita los de YouTube. Tus consultas de dominios pasan por AdGuard. Si en alguna red " +
                    "(hotel, empresa) te quedas sin internet, desactívalo. Al desactivarlo vuelve tu DNS anterior.",
                    fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp
                )
                if (!granted) {
                    Text("Requiere el modo avanzado", fontSize = 12.sp, color = AccentOrange)
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
                    "PhoneOptimizer es 100% gratuita y sin anuncios. " +
                    "Si te resultó útil, puedes apoyar su desarrollo " +
                    "con una contribución voluntaria — cada aporte ayuda a seguir mejorando la app.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
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
                            "1165745950",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange
                        )
                    }
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString("1165745950")) },
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
                            "0xb6f6731a4ea87f8e1fd6f44f48b5bc4204571f08",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange
                        )
                    }
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString("0xb6f6731a4ea87f8e1fd6f44f48b5bc4204571f08")) },
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
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
                InfoRow("Versión", BuildConfig.VERSION_NAME)
                InfoRow("Desarrollado por", "Enmanuel Gil")
                InfoRow("Compatibilidad", "Android 8.0+ (API 26)")
                InfoRow("Sin anuncios ni internet", "Gratis · sin root")
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
                Text(
                    "Diagnóstico honesto del teléfono (espacio, batería, temperatura, memoria), consejos " +
                    "que ayudan de verdad y cierre de apps en segundo plano. No borra datos ni archivos, " +
                    "y todos los ajustes que cambia se pueden restaurar.",
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            try {
                                ctx2.startActivity(Intent(Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://github.com/EnMaNueL-G")))
                            } catch (_: Exception) {}
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Text("github.com/EnMaNueL-G", fontSize = 13.sp, color = PrimaryBlue, fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
                Text("Enmanuel Gil · OptiSuite", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("por Enmanuel Gil (EnMaNueL-G)", fontSize = 12.sp, color = TextSecondary)
                Text("© 2026 OptiSuite", fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.7f))
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
                .clickable { onCopy(command) }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                command, fontSize = 11.sp, color = AccentGreen,
                fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onCopy(command) }) {
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
