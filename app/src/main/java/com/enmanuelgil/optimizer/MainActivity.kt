package com.enmanuelgil.optimizer

import android.app.Application
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import com.enmanuelgil.optimizer.service.MaintenanceScheduler
import com.enmanuelgil.optimizer.service.ThermalMonitorService
import com.enmanuelgil.optimizer.ui.screens.*
import com.enmanuelgil.optimizer.ui.theme.*
import com.enmanuelgil.optimizer.viewmodel.MainViewModel

class OptimizerApp : Application()

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tema oscuro fijo: iconos claros en las barras aunque el sistema esté en modo claro
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        if (savedInstanceState == null) {
            // Solo si el usuario los activó (antes se encendían siempre al abrir la app)
            ThermalMonitorService.startIfEnabled(this)
            MaintenanceScheduler.applyFromPrefs(this)
        }
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setContent {
            PhoneOptimizerTheme { PhoneOptimizerApp(viewModel) }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startMonitoring()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }

    override fun onStop() {
        viewModel.stopMonitoring()   // no leer nada con la app en segundo plano
        super.onStop()
    }
}

private val TABS = listOf(
    NavTab("Panel", Icons.Default.Dashboard),
    NavTab("Optimizar", Icons.Default.FlashOn),
    NavTab("Apps", Icons.Default.PhoneAndroid),
    NavTab("Ajustes", Icons.Default.Settings)
)

@Composable
fun PhoneOptimizerApp(viewModel: MainViewModel) {
    val stats            by viewModel.stats.collectAsStateWithLifecycle()
    val isOptimizing     by viewModel.isOptimizing.collectAsStateWithLifecycle()
    val progress         by viewModel.optimizationProgress.collectAsStateWithLifecycle()
    val lastResult       by viewModel.lastResult.collectAsStateWithLifecycle()
    val selectedProfile  by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val privilegesStatus by viewModel.privilegesStatus.collectAsStateWithLifecycle()
    val adBlockEnabled   by viewModel.adBlockEnabled.collectAsStateWithLifecycle()
    val canRestore       by viewModel.canRestore.collectAsStateWithLifecycle()
    val topApps          by viewModel.topApps.collectAsStateWithLifecycle()
    val hasUsageAccess   by viewModel.hasUsageAccess.collectAsStateWithLifecycle()
    val isLoadingApps    by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val history          by viewModel.optimizationHistory.collectAsStateWithLifecycle()
    val message          by viewModel.message.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val windowInfo = currentWindowAdaptiveInfo()
    val isTablet = windowInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val screenContent: @Composable (PaddingValues) -> Unit = { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(paddingValues)
        ) {
            // En tablets, contenido centrado con ancho máximo legible
            Box(Modifier.fillMaxHeight().widthIn(max = 640.dp).align(androidx.compose.ui.Alignment.TopCenter)) {
                when (currentTab) {
                    0 -> DashboardScreen(
                        stats = stats,
                        onGoOptimize = { currentTab = 1 }
                    )
                    1 -> OptimizeScreen(
                        selectedProfile = selectedProfile,
                        onProfileSelected = viewModel::setProfile,
                        isOptimizing = isOptimizing,
                        progress = progress,
                        lastResult = lastResult,
                        privilegesStatus = privilegesStatus,
                        canRestore = canRestore,
                        onRestore = viewModel::restoreSettings,
                        onOptimize = viewModel::optimize,
                        onCloseBackground = viewModel::closeBackgroundApps
                    )
                    2 -> AppsScreen(
                        topApps = topApps,
                        hasUsageAccess = hasUsageAccess,
                        isLoading = isLoadingApps,
                        history = history,
                        onRefreshApps = viewModel::loadTopApps,
                        onGrantUsage = viewModel::openUsageAccess,
                        onClearHistory = viewModel::clearHistory
                    )
                    3 -> SettingsScreen(
                        privilegesStatus = privilegesStatus,
                        adBlockEnabled = adBlockEnabled,
                        canRestore = canRestore,
                        onAdBlockToggle = viewModel::toggleAdBlock,
                        onRestore = viewModel::restoreSettings
                    )
                }
            }
        }
    }

    if (isTablet) {
        Scaffold(
            containerColor = BackgroundDark,
            snackbarHost = { SnackbarHost(snackbar) }
        ) { pv ->
            Row(Modifier.fillMaxSize().background(BackgroundDark).padding(pv)) {
                NavigationRail(containerColor = SurfaceDark, modifier = Modifier.fillMaxHeight()) {
                    Spacer(Modifier.height(16.dp))
                    TABS.forEachIndexed { index, tab ->
                        NavigationRailItem(
                            selected = currentTab == index,
                            onClick = { currentTab = index },
                            icon = { Icon(tab.icon, contentDescription = null,
                                tint = if (currentTab == index) PrimaryBlue else TextSecondary) },
                            label = { Text(tab.label,
                                color = if (currentTab == index) PrimaryBlue else TextSecondary,
                                fontWeight = if (currentTab == index) FontWeight.SemiBold else FontWeight.Normal) },
                            colors = NavigationRailItemDefaults.colors(indicatorColor = PrimaryBlue.copy(alpha = 0.15f))
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                Box(Modifier.fillMaxSize()) { screenContent(PaddingValues(0.dp)) }
            }
        }
    } else {
        Scaffold(
            containerColor = BackgroundDark,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = SurfaceDark, tonalElevation = 0.dp) {
                    TABS.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = currentTab == index,
                            onClick = { currentTab = index },
                            icon = { Icon(tab.icon, contentDescription = null,
                                tint = if (currentTab == index) PrimaryBlue else TextSecondary) },
                            label = { Text(tab.label,
                                color = if (currentTab == index) PrimaryBlue else TextSecondary,
                                fontWeight = if (currentTab == index) FontWeight.SemiBold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryBlue.copy(alpha = 0.15f))
                        )
                    }
                }
            }
        ) { paddingValues -> screenContent(paddingValues) }
    }
}

data class NavTab(val label: String, val icon: ImageVector)
