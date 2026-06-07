package com.enmanuelgil.optimizer

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.enmanuelgil.optimizer.service.ThermalMonitorService
import com.enmanuelgil.optimizer.ui.screens.*
import com.enmanuelgil.optimizer.ui.theme.*
import com.enmanuelgil.optimizer.viewmodel.MainViewModel

class OptimizerApp : Application() {
    override fun onCreate() { super.onCreate() }
}

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThermalMonitorService.start(this)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setContent {
            PhoneOptimizerTheme {
                PhoneOptimizerApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) viewModel.checkPrivileges()
    }
}

@Composable
fun PhoneOptimizerApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val stats             by viewModel.stats.collectAsStateWithLifecycle()
    val isOptimizing      by viewModel.isOptimizing.collectAsStateWithLifecycle()
    val progress          by viewModel.optimizationProgress.collectAsStateWithLifecycle()
    val lastResult        by viewModel.lastResult.collectAsStateWithLifecycle()
    val selectedProfile   by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val privilegesStatus  by viewModel.privilegesStatus.collectAsStateWithLifecycle()
    val adBlockEnabled    by viewModel.adBlockEnabled.collectAsStateWithLifecycle()
    val topApps           by viewModel.topApps.collectAsStateWithLifecycle()
    val isLoadingApps     by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val history           by viewModel.optimizationHistory.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) }

    // Adaptive layout: use NavigationRail on tablets (medium/expanded width)
    val windowInfo  = currentWindowAdaptiveInfo()
    val isTablet    = windowInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val tabs = listOf(
        NavTab("Panel",     Icons.Default.Dashboard),
        NavTab("Optimizar", Icons.Default.FlashOn),
        NavTab("Apps",      Icons.Default.PhoneAndroid),
        NavTab("Ajustes",   Icons.Default.Settings)
    )

    val screenContent: @Composable (PaddingValues) -> Unit = { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(paddingValues)
        ) {
            when (currentTab) {
                0 -> DashboardScreen(
                    stats = stats,
                    isOptimizing = isOptimizing,
                    onQuickOptimize = viewModel::optimize
                )
                1 -> OptimizeScreen(
                    selectedProfile = selectedProfile,
                    onProfileSelected = viewModel::setProfile,
                    isOptimizing = isOptimizing,
                    progress = progress,
                    lastResult = lastResult,
                    privilegesStatus = privilegesStatus,
                    onOptimize = viewModel::optimize
                )
                2 -> AppsScreen(
                    topApps = topApps,
                    isLoading = isLoadingApps,
                    history = history,
                    onRefreshApps = viewModel::loadTopApps,
                    onForceStop = viewModel::forceStopApp,
                    onClearHistory = viewModel::clearHistory
                )
                3 -> SettingsScreen(
                    privilegesStatus = privilegesStatus,
                    adBlockEnabled = adBlockEnabled,
                    onAdBlockToggle = viewModel::toggleAdBlock,
                    onStartMonitor = { ThermalMonitorService.start(context) },
                    onStopMonitor  = { ThermalMonitorService.stop(context) }
                )
            }
        }
    }

    if (isTablet) {
        // ── TABLET: NavigationRail en el lateral izquierdo ─────────────────
        PhoneOptimizerTheme {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDark)
            ) {
                NavigationRail(
                    containerColor = SurfaceDark,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(Modifier.height(16.dp))
                    tabs.forEachIndexed { index, tab ->
                        NavigationRailItem(
                            selected  = currentTab == index,
                            onClick   = { currentTab = index },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (currentTab == index) PrimaryBlue else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    tab.label,
                                    color      = if (currentTab == index) PrimaryBlue else TextSecondary,
                                    fontWeight = if (currentTab == index) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = PrimaryBlue.copy(alpha = 0.15f)
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                // Content takes the rest of the screen
                Box(modifier = Modifier.fillMaxSize()) {
                    screenContent(PaddingValues(0.dp))
                }
            }
        }
    } else {
        // ── TELÉFONO: NavigationBar en la parte inferior ───────────────────
        Scaffold(
            containerColor = BackgroundDark,
            bottomBar = {
                NavigationBar(containerColor = SurfaceDark, tonalElevation = 0.dp) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected  = currentTab == index,
                            onClick   = { currentTab = index },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (currentTab == index) PrimaryBlue else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    tab.label,
                                    color      = if (currentTab == index) PrimaryBlue else TextSecondary,
                                    fontWeight = if (currentTab == index) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = PrimaryBlue.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            screenContent(paddingValues)
        }
    }
}

data class NavTab(val label: String, val icon: ImageVector)
